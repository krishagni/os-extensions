<?php
error_reporting ( E_ALL );

// Disable REDCap's authentication (will use API tokens for authentication)
define ( "NOAUTH", true );

// Set constant to denote that this is an API call
define ( "API", true );

// Include REDCap Connect
include "../redcap_connect.php";

$token = (isset($_POST['token']) ? $_POST['token'] : null);
$query = 
  "select
     project_id, username from redcap_user_rights
   where
     data_logging = 1 and api_token = '" . db_real_escape_string($token) . "';";
$rs = db_query($query);
if (db_num_rows($rs) == 0) {
  error("Invalid API token");
}

$userRow   = db_fetch_assoc($rs);
$projectId = $userRow['project_id'];
$username  = $userRow['username'];

$content   = (isset($_POST['content']) ? $_POST['content'] : 'data_audit_log');
if (strcmp($content, 'data_audit_log') == 0) {
  $lastEventId   = (isset($_POST['lastEventId']) ? $_POST['lastEventId'] : 0);
  $maxEvents     = (isset($_POST['maxEvents']) ? $_POST['maxEvents'] : 100);
  $recordIds     = (isset($_POST['recordIds']) ? $_POST['recordIds'] : '');
  send_data_audit_log($projectId, $lastEventId, $maxEvents, $recordIds);
} else if (strcmp($content, 'modified_record_ids') == 0) {
  $lastEventId   = (isset($_POST['lastEventId']) ? $_POST['lastEventId'] : 0);
  send_modified_record_ids($projectId, $lastEventId);
} else if (strcmp($content, 'data_record_values') == 0) {
  $lastRowId = (isset($_POST['lastRowId']) ? $_POST['lastRowId'] : 0);
  $maxRows   = (isset($_POST['maxRows']) ? $_POST['maxRows'] : 100);
  $recordIds = (isset($_POST['recordIds']) ? $_POST['recordIds'] : '');
  send_data_record_values($projectId, $lastRowId, $maxRows, $recordIds);
} else if (strcmp($content, 'event') == 0) {
  send_events($projectId);
} else if (strcmp($content, 'event_forms') == 0) {
  send_event_forms($projectId);
} else if (strcmp($content, 'latest_log_event_id') == 0) {
  send_latest_log_event_id($projectId);
} else if (strcmp($content, 'version') == 0) {
   $result = array();
   $result["version"] = "2021-10-11T09:30:00.000Z";
   header("Content-Type:application/json");
   print json_encode($result);
}

function send_data_audit_log($projectId, $startEventId, $maxEvents, $recordIds) {
  $logEventTable = get_audit_log_table($projectId);

  $query =
    "select
       le.log_event_id, le.ts, le.event, le.pk, le.event_id, e.descrip as event_name, le.data_values
     from
    " . $logEventTable . " le
       inner join redcap_events_metadata e on e.event_id = le.event_id
     where
       le.object_type = 'redcap_data' and
       le.project_id = " . db_real_escape_string($projectId);

  if (strcmp($onlyDelEvents, 'true') == 0) {
    $query = $query . " and (le.event = 'DELETE' or (le.event = 'UPDATE' and le.description like 'Delete%'))";
  } else {
    $query = $query . " and le.event in ('INSERT', 'UPDATE', 'DELETE')";
  }

  if (!empty($recordIds)) {
    $query = $query . " and le.pk in (" . $recordIds . ")";
  }

  if ($startEventId > 0) {
    $query = $query . " and log_event_id > " . db_real_escape_string($startEventId);
  }

  $query = $query . " order by log_event_id limit " . db_real_escape_string($maxEvents);

  send_records(db_query($query));
}

function send_modified_record_ids($projectId, $startEventId) {
  $logEventTable = get_audit_log_table($projectId);

  $query =
    "select
       le.pk, le.event
     from
    " . $logEventTable . " le
       left join " . $logEventTable . " te on te.pk = le.pk and le.log_event_id < te.log_event_id
     where
       le.object_type = 'redcap_data' and
       le.project_id = " . db_real_escape_string($projectId) . " and
       le.event in ('INSERT', 'UPDATE', 'DELETE') and
       te.log_event_id is null";


  if ($startEventId > 0) {
    $query = $query . " and le.log_event_id > " . db_real_escape_string($startEventId);
  }

  $query = $query . " order by le.log_event_id";
  send_records(db_query($query));
}

function get_audit_log_table($projectId) {
  $table = "redcap_log_event";

  try {
    $query =
      "select
         log_event_table
       from
         redcap_projects
       where
         project_id = " . db_real_escape_string($projectId);

    $rs = db_query($query);
    $row = db_fetch_assoc($rs);
    if (!empty($row) && !empty($row["log_event_table"])) {
      $table = $row["log_event_table"];
    }
  } catch (Exception $e) {
    echo "Running on REDCap < 9.6.0\n";
  }

  return $table;
}

function send_data_record_values($projectId, $startRowId, $maxRows, $recordIds) {
  $query =
    "select
       d.record, d.event_id, d.instance, d.field_name, d.value, m.element_type
     from
       redcap_data d
       inner join redcap_metadata m on m.field_name = d.field_name and m.project_id  = d.project_id
       left join redcap_events_metadata e on e.event_id = d.event_id
     where
       d.project_id = " . db_real_escape_string($projectId);

  if (!empty($recordIds)) {
    $query = $query . " and d.record in (" . $recordIds . ")";
  }

  $query = $query . " order by abs(d.record), d.record, d.event_id, d.instance limit";
  if ($startRowId > 0) {
    $query = $query . " " . db_real_escape_string($startRowId);
  } else {
    $query = $query . " 0";
  }

  if ($maxRows > 0) {
    $query = $query . ", " . db_real_escape_string($maxRows);
  } else {
    $query = $query . ", 100";
  }

  $records = array();
  $rids    = array();
  $eids    = array();

  $rs = db_query($query);
  while ($row = db_fetch_assoc($rs)) {
    $records [] = $row;
    $rids[$row["record"]] = true;
    $eids[$row["event_id"]] = true;
  }

  $tsMap = get_record_event_ts($projectId, $rids, $eids);
  foreach ($records as $idx => $record) {
    $records[$idx]["ts"] = $tsMap[$record["record"] . ":" . $record["event_id"]];
  }

  header("Content-Type:application/json");
  print json_encode($records);
}

function get_record_event_ts($projectId, $rids, $eids) {
  $tsMap = array();
  if (count($rids) == 0) {
    return $tsMap;
  }

  $logEventTable = get_audit_log_table($projectId);

  $query = "
    select
      pk, event_id, min(ts) as ts
    from
    " . $logEventTable . "
    where
      pk in (" . to_csv($rids) . ") and
      event_id in (" . to_csv($eids) . ") and
      project_id = " . db_real_escape_string($projectId) . " and
      event in ('INSERT', 'UPDATE', 'DELETE')
    group by
      pk, event_id";

  $rs = db_query($query);
  while ($row = db_fetch_assoc($rs)) {
    $tsMap[$row["pk"] . ":" . $row["event_id"]] = $row["ts"];
  }

  return $tsMap;
}

function to_csv($array) {
  $result = "";
  foreach ($array as $key => $value) {
    if (strlen($result) > 0) {
      $result = $result . ",";
    }

    $result = $result . "'" . $key . "'";
  }

  return $result;
}

function send_events($projectId) {
  $query =
    "select
       e.event_id, e.day_offset, e.offset_min, e.offset_max,
       e.descrip as event_name, a.arm_num,
       case when er.repeatable = 1 then 1 else 0 end as repeatable
     from
       redcap_events_metadata e
       inner join redcap_events_arms a on a.arm_id = e.arm_id
       left join (
         select
           event_id, 1 as repeatable
         from
           redcap_events_repeat
         where
           form_name is null or
           length(trim(form_name)) = 0
       ) er on er.event_id = e.event_id
     where
       a.project_id = " . db_real_escape_string($projectId);

  send_records(db_query($query));
}

function send_event_forms($projectId) {
  $query =
    "select
       ef.event_id, ef.form_name, e.descrip as event_name, a.arm_num
     from
       redcap_events_forms ef
       inner join redcap_events_metadata e on e.event_id = ef.event_id
       inner join redcap_events_arms a on a.arm_id = e.arm_id
     where
       a.project_id = " . db_real_escape_string($projectId);

  send_records(db_query($query));
}

function send_latest_log_event_id($projectId) {
  $logEventTable = get_audit_log_table($projectId);

  $query =
    "select
       max(log_event_id) as id
     from
    " . $logEventTable . "
     where
       project_id = " . db_real_escape_string($projectId);

  $rs = db_query($query);
  $row = db_fetch_assoc($rs);

  header("Content-Type:application/json");
  print json_encode($row);
}

function send_records($rs) {
  $records = array();
  while ($row = db_fetch_assoc($rs)) {
    $records [] = $row;
  }

  header("Content-Type:application/json");
  print json_encode($records);
}

function error($msg) {
	http_response_code(400);
	die(json_encode(array('error' => $msg)));
}
?>
