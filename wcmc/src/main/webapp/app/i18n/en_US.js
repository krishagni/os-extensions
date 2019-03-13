{
    "cp": {
        "repositories": "Institution(s)",
        "coordinators": "Study Coordinator(s)",
        "date": "",
        "type": "Study Focus",
        "code": "Study Code",
        "participant_centric": "Participant",
        "specimen_centric": "Specimen",
        "ethics_approval_id": "IRB #",
        "sop_document_file": "ICF Template",
        "store_sprs": "SPRs",
        "anticipated_participant_count": "# Participants",
        "visit_name_fmt": "Event Name Format",
        "on_visit_completion": "On Event Completion",
        "visit_names": "Event Names",
        "visit_print_settings": "Event Print Settings",
        "sop_document": "SOP Document",
        "sop_document_url": "SOP Document URL",
          "sop_document_file": "ICF Template",
        "specimen_label_fmt": "IBC Code Format",
        "spec_labels": "IBC Codes",
        "self": "Study",
        "list": "Studies",
        "create_cp_title": "Create Study",
        "import_cp_title": "Import Study",
        "cp_def_file": "Study Definition File",
        "disable_pre_print_warning": "Turning off pre-printing at Study level will turn it off for all specimen requirements too",
        "delete_cps":  "Are you sure you want to delete selected Studies?",
        "cps_deleted": "Studies deleted successfully",
        "cps_delete_pending": "Studies deletion taking longer time than anticipated. You'll receive emails when it gets completed",
        "no_cpes": "There are no Study events to show. Please create an event by clicking on Add Event ...",
        "ppid_fmt": "Registry ID Format",
        "ppids": "Registry IDs",
         "repositories": "Site(s)",
         "extract_spr_text": "Extract CoPath SPR",
        "tooltip": {
              "view_details": "Click to view Study details",
              "add": "Click to add new Study",
              "edit": "Click to edit Study",
              "search": "Click to search Studies"
            },
            "label_format":
            {
              "ppid": "Registry ID"
            },
            "dp": {
              "dup_dp": "Distribution protocol already added to the study.",
              "no_dp": "No distribution protocols can be assigned to study."
            }
    },

    "menu": {
        "cp_desc": "Create, update SOP of events and specimens",
        "rde_desc": "Rapidly capture patients, events and specimens data",
        "collection_protocols": "Studies",
        "institutes": "Institutions",
        "sites": "Site(s)",
        "sites_desc": "Add and update site(s)"
    },

    "label_format": {
        "visit": "Event"
    },

    "forms": {
        "SpecimenCollectionGroup": "Event"
    },

    "spmn_label_pre_print_modes": {
        "ON_VISIT": "On Event"
    },

    "participant": {
    	"register_participant": "Create Participant",
        "visits_count": "Events",
        "spmn_label_or_barcode": "IBC Code / Barcode",
        "collection_protocol": "Study",
        "ppid": "Registry ID",
        "protocol_id": "Registry ID",
        "ppids": "Registry IDs",
        "ppids_csv": "Comma separated list of Registry IDs",
        "menu" : {
        	"visits": "Events"
        },
        "tooltip": {
                  "specimen_catalog": "Click to view study specimens"
                },
                "buttons": {
                  "register_n_collect": "Create Events"
                }
    },

    "tooltip": {
        "completed_visits_count": "Count of Completed Events"
    },

    "visits": {
        "title": "Event",
        "list": "Events",
        "occurred_visits": "Occurred Events",
        "anticipated_visits": "Pending Events",
        "missed_or_not_collected_visits": "Missed/Not Collected Events",
        "visit_status": "Status",
        "visit_date": "Event Date",
        "anticipated_visit_date": "Anticipated Event Date",
        "visit_site": "Event Site",
        "reason_for_missed_visit": "Reason",
        "unplanned_visit": "Unplanned Event",
        "names": "Event Names",
        "names_csv": "Comma separated list of event names",
        "names_print_job_created": "Event names print job {{jobId}} created successfully",
        "search_results": "Event Search Result for {{key}}",
        "surgical_path_no": "Accession #",
        "name": "Event IBC Code",
        "add": "",
        "update": "",
        "ctx_menu": {
            "add_visit": "Add Event",
            "view_visit": "View Event",
            "edit_visit": "Edit Event",
            "new_visit": "New Event",
            "missed_visit": "Missed Event",
            "print_specimen_labels": "Print IBC Codes"
        }
    },

    "specimens": {
        "visit_details": "Event Details",
        "visit_name": "Event Name",
        "visit_date": "Event Date",
        "visit_site": "Event Site",
        "labels_csv": "Comma separated list of IBC Codes",
        "labels_print_job_created": "IBC Codes print job {{jobId}} created successfully",
        "confirm_print_q": "Do you want to print child IBC Codes as well?",
        "too_many_specimens": "Too many IBC Codes/barcodes provided. Please try by removing some labels/barcodes",
        "create_aliquots_q": "Do you want to create aliquot(s)?",
        "create_aliquots": "Create Aliquot(s)",
        "create_derivatives": "Create Derivative(s)",
        "cp": "Study",
        "ppid": "Registry ID",
        "list": "Child Specimens",
        "qty": "Volume",
        "errors": {
	        "visit_not_completed": "Please complete the event before collecting unplanned specimen",
	        "select_same_visit_spmns": "Please select specimens of same event"
	    },
      "tooltip": {
        "print": "Print IBC Codes"
      },
      "tree_node_statuses": {
        "unplanned": "Stored"
      },
      "buttons": {
      "create_derivative": "Create Derivative(s)"
    }
    },

    "role": {
	    "resources": {
	        "VisitAndSpecimen": "Events and Specimens",
          "CollectionProtocol": "Studies"
	    }
    },

    "extensions": {
        "bulk_import_visit_extns": "Bulk Import Event Forms"
    },

    "queries": {
        "visits": "Events",
        "select_cp": "Select a Study",
        "search_cp": "Search Study"
    },

    "entities": {
        "visit": "Event",
        "specimen_event": "Specimen Event",
        "visit_extension": "Event Custom Fields",
        "collection_protocol": "Study",
        "collection_protocol_event": "Study Event",
        "collection_protocol_registration": "Study Registration",
        "participant_reg": "Study Registration",
        "cp_extension": "Study Custom Fields",
        "institute": "Institution"
    },

    "bulk_imports": {
    	"object_types": {
        	"visit": "Events",
          "cpr": "Study Registrations",
          "institute": "Institutions",
          "site": "Site(s)"
        }
    },

    "settings": {
	    "biospecimen": {
	        "visit_label_print_rules": "Event Label Print Rules",
	        "visit_label_printer": "Event Label Printer Implementor",
	        "pending_spmns_disp_interval_desc": "Pending specimens will be automatically hidden in specimen tree after specified number of days have elapsed since collection of parent specimen or event",
	        "visits_lookup_flow": "Events Lookup Workflow",
	        "visits_lookup_flow_desc": "Spring bean name or fully qualified name of class implementing customised events lookup workflow. For example: bean: customisedVisitsLookup or class: com.krishagni.openspecimen.plugins.CustomisedVisitsLookup",
	        "store_spr_desc": "Enable or disable uploading of surgical pathology reports for an event.",
          "cp_sop_doc_dir": "SOP Documents Directory",
          "cp_sop_doc_dir_desc": "Folder on server to store study SOP Documents",
          "cp_sop_doc_url_desc": "System level study SOP Document link",
          "cp_sop_doc": "CP SOP Document",
          "cp_sop_doc_desc": "System level study SOP Document",
          "specimen_label_print_rules": "IBC Code Print Rules",
          "specimen_label_printer": "IBC Code Printer Implementor",
          "unique_spmn_label_per_cp": "Unique IBC Code per CP",
          "unique_spmn_label_per_cp_desc": "Enable or Disable uniqueness of IBC Codes at CP level. Enabling this setting ensures IBC Codes are unique only within Study. Disabling this setting ensures no two IBC Codes are same throughout the system.",
          "cp_coding_enabled_desc": "Enable or disable short codes for study, events, and specimen requirements. Needed for barcoding.",
          "cp_expiry_rem_notif_desc": "Number of days prior to study expiry when email notification should be sent to the PI.",
          "cp_expiry_rem_rept_inter_desc": "Number of days after which email notification should be repeated for study expiry.",
          "cp_sop_doc_dir_desc": "Folder on server to store study SOP Documents",
          "cp_sop_doc_url_desc": "System level study SOP Document link",
          "cp_sop_doc_desc": "System level study SOP Document",
          "sys_cp_rpt_settings_desc": "System level study report settings. These settings are used when CP specific reporting settings are not specified",
          "sys_spmn_cp_rpt_settings_desc": "System level specimen centric study report settings. These settings are used when CP specific reporting settings are not specified. If this setting is also not specified then settings for regular CP reports is used",
          "mrn_restriction_enabled_desc": "Restrict access to participants based on the user's site(s). By default all participants within a protocol are displayed."


	    },
      "administrative": {
         "allow_spmn_relabeling_desc": "Enable to allow editing of IBC Code when shipments are received.",
          "download_labels_print_file_desc": "Enable or disable downloading of IBC Code print integration data file. When enabled, a CSV file containing the specimens data to be printed on labels (stickers) is downloaded to the user computer. When disabled, the integration file is not downloaded."
      }
    },
    "dp":{
    	"irb_id": "IRB #",
    	"date": "",
    	"coordinators": "Study Coordinator(s)",
    	"label_fmt": "Study Code",
    	"custom_fields_form": "Custom Fields",
    	"order_report_query": "Query Report",
    	"distributing_sites": "Distribution",
      "institute": "Institution",
      "receiving_institute": "Institution",
      "receiving_site": " Site",
       "distributing_sites": "Distributing Site(s)",
       "all_sites": "All current and future site(s)",
       "dist_inst_pre_selected": "Distributing institution {{institution}} already selected in row {{rowNo}}"
   },
    "srs":
    {
    "lbl_fmt_required_for_auto_print": "Pre-printing requires IBC Code format to be specified either at CP level or requirement level",
     "create_aliquots": "Create Aliquot(s)",
      "buttons": {
                "create_aliquots": "Create Aliquot(s)",
                "create_derivative": "Create Derivative(s)"
              },
      "ctx_menu": {
                "create_aliquots": "Create Aliquot(s)",
                 "create_derivatives": "Create Derivative(s)"
              },
      "errors": {
                "insufficient_qty": "Insufficient parent requirement quantity to create aliquot(s)"
                }
    },

    "container":
    {
    "paste_specimen_labels": "Paste or input IBC Codes or barcodes separated by comma, tab, or newline",
    "no_free_locs": "Container does not have enough free locations to accommodate input IBC Codes",
    "collection_protocol": "Studies",
    "type": "Type(s)",
    "cell_display_props": {
          "SPECIMEN_LABEL": "IBC Code",
           "SPECIMEN_PPID" : "Registry ID"
        }
    },
"specimen_list":
{
  "enter_specimen_label": "Enter IBC Codes or barcodes separated by a comma, tab or newline",
  "cp": "Study",
  "create_aliquots": "Create Aliquot(s)",
  "create_derivatives": "Create Derivative(s)",
  "no_specimens_to_create_aliquots": "Please select at least one collected parent specimen to create aliquot(s)",
  "ppid": "Registry ID"
},
"site":
{
  "cp_count": "Studies",
  "tooltip":
  {
    "cp_count": "Count of Studies"
  }
},
"user":
{
    "institute": "Institution",
     "inst_admin": "Institution Administrator",
    "role": {
              "cp": "Study"
            },
    "tooltip":
            {
            "cp_count": "Studies",
            "institute_name": "Institution"
          },
          "types": {

            "INSTITUTE": "Institution Administrator"
   }

},
"form": {
   "not_used": "Not used in any studies",
   "used_in": "Used in following studies",
   "collection_protocol": "Study",
   "use_in_cps": "Use form in following studies",
   "spe_attached_to_all_cps": "Specimen Event forms always attached to all studies.",
   "all_cps": "All Present and Future Studies",
   "select_cps": "Select One or More Studies",
   "tooltip": {
      "attach_form_to_cp": "Click to attach form to a Study"
    }
  },
  "orders": {
    "receiving_institute": "Institution",
     "create_order": "Create Distribution",
     "details": "Distribution Details",
     "execution_date": "Distribution Date",
      "requestor": "Requester",
      "name": "Title",
      "sender_comments": "Comments",
    "spec": {
    "cp": "Study",
    "label": "IBC Code",
     "desc": "Specimen Type"
  }
},
  "notifications": {
"email_cp_expiring_notification_desc": "Send reminder emails when study is about to expire.",
"cp_setup_help_link": "Study Setup",
"cp_setup_help_link_desc": "Link to training materials on how to setup (create, update) studies, events, specimen requirements, consents, label formats, and many more"
  },

"training": {
"cp_help_link": "Study",
"cp_help_link_desc": "Link to training materials on how to manage studies",
"institute_help_link": "Institution",
"institute_help_link_desc": "Link to training materials on how to manage institutions",
"site_help_link_desc": "Link to training materials on how to manage site(s)"
},
"institute": {
  "list": "Institutions",
  "create_institute": "Create Institution",
  "update_institute": "Update Institution",
  "delete_institutes":  "Are you sure you want to delete selected institutions?",
  "institutes_deleted": "Institutions deleted successfully",
  "bulk_import": "Import Institutions",
  "bulk_import_jobs": "Import Institutions Job List",
  "tooltip": {
    "view_details": "Click to view Institution details",
    "add": "Click to add new Institution",
    "edit": "Click to edit institution",
    "delete_institutes": "Click to delete selected institutions",
    "user_count": "Count of Users in Institution",
    "search": "Click to search Institutions"
  }
},
"site": {
    "list": "Site(s)",
    "institute": "Institution",
    "all_sites": "All current and future site(s)",
    "delete_sites":  "Are you sure you want to delete selected site(s)?",
    "sites_deleted": "Site(s) deleted successfully",
    "bulk_import": "Import Site(s)",
    "bulk_import_jobs": "Import Site(s) Job List",
    "tooltip": {
     "delete_sites": "Click to delete selected site(s)",
     "search": "Click to search Site(s)"
   }
 },
  "shipments": {
     "receiving_institute": "Institution",
     "multi_site_specimens": "Can't ship specimens from multiple storage site(s)"
  },
  "common": {
    "buttons": {
       "submit": "Create"
    }
  }
}
