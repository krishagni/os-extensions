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
  $lastEventId = (isset($_POST['lastEventId']) ? $_POST['lastEventId'] : 0);
  $maxEvents   = (isset($_POST['maxEvents']) ? $_POST['maxEvents'] : 100);
  send_data_audit_log($projectId, $lastEventId, $maxEvents);
} else if (strcmp($content, 'event') == 0) {
  send_events($projectId);
} else if (strcmp($content, 'event_forms') == 0) {
  send_event_forms($projectId);
}

function send_data_audit_log($projectId, $startEventId, $maxEvents) {
  $query =
    "select
       le.log_event_id, le.ts, le.event, le.pk, le.event_id, e.descrip as event_name, le.data_values
     from
       redcap_log_event le
       inner join redcap_events_metadata e on e.event_id = le.event_id
     where
       le.object_type = 'redcap_data' and
       le.event in ('INSERT', 'UPDATE', 'DELETE') and
       le.project_id = " . db_real_escape_string($projectId);

  if ($startEventId > 0) {
    $query = $query . " and log_event_id > " . db_real_escape_string($startEventId);
  }

  $query = $query . " order by log_event_id limit " . db_real_escape_string($maxEvents);

  send_records(db_query($query));
}

function send_events($projectId) {
  $query =
    "select
       e.event_id, e.day_offset, e.offset_min, e.offset_max, e.descrip as event_name, a.arm_num
     from
       redcap_events_metadata e
       inner join redcap_events_arms a on a.arm_id = e.arm_id
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
