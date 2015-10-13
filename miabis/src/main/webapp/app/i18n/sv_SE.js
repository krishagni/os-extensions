{
  "menu": {
    "collection_protocols": "Studies",
    "specimen_lists": "Sample Lists",
    "cp_desc": "Create, update SOP of visits and samples",
    "dp_desc": "Create, update procedures for distributing samples",
    "specimen_lists_desc": "Create, share and manage sample lists",
    "distribution_orders_desc": "Create, execute request orders for distributing samples"
  },

  "cp": {
    "list": "Studies",
    "create_cp_title": "Create Study",
    "import_cp_title": "Import Study",
    "cp_def_file": "Study Definition File",
    "no_cpes": "There are no Study events to show. Please create an event by clicking on Add Event...",
    "confirm_delete_event": "Study Event and its sample requirements will be deleted. Are you sure you want to proceed?",
    "title": "Study Name",
    "coordinators": "Contact information",
    "ethics_approval_id": "Study ID",
    "specimen_label_fmt": "Sample ID Format",
    "derivative_label_fmt": "Derivative ID Format",
    "aliquot_label_fmt": "Aliquot ID Format",
    "spec_labels": "Sample IDs",
    "confirm_delete_event": "Collection Protocol Event and its sample requirements will be deleted. Are you sure you want to proceed?",
    "tooltip": {
      "view_details": "Click to view/edit Study details",
      "collected_specimens_count": "Count of Collected Samples",
      "add": "Click to add new Study",
      "search": "Click to search Studies"
    },
    "label_format": {
      "specimen": "Sample"
    }
  }, 
  
  "site": {
    "code": "Acronym",
    "coordinators": "Contact Information",
    "tooltip": {
    "cp_count": "Count of Studies"
    }
  },
  
  "role": {
    "resources" : {
    "VisitAndSpecimen": "Visits and Samples",
    "CollectionProtocol": "Studies"
    }
  },

  "form": {
    "all_cps": "All Present and Future Studies",
    "select_cps": "Select One or More Studies",
    "not_used": "Not used in any Studies",
    "used_in": "Used in following Studies",
    "collection_protocol": "Study",
    "use_in_cps": "Use form in following Studies",
    "spe_attached_to_all_cps": "Samples Event forms always attached to all studies.",
    "tooltip": {
      "attach_form_to_cp": "Click to attach form to a Study"
    }
  },
  
  "participant": {
    "collection_protocol": "Study",
    "uid": "Personal Identification Number",
    "id": "MPI / MRN / PIN",
    "uid_short": "PIN",
    "gender": "Sex",
    "ethnicity": "Ethnic Group",
    "spmn_label_or_barcode": "Sample ID / Barcode",
    "matching_attr": {
      "uid": "PIN"
    },
    "tooltip": {
      "collected_specimens_count": "Count of Collected Samples"
    }
  },
 
  "user": {
    "role": {
      "cp": "Study"
    },
    "tooltip": {
      "cp_count": "Study"
    }
  },
  
  "specimen_list": { 
    "cp": "Study",
    "new_list": "New Sample List",
    "create_list": "Create Sample List",
    "update_list": "Update Sample List",
    "create_new_list": "Create New Sample List",
    "search_list": "Search Sample List",
    "lists": "Sample Lists",
    "more_specimens": "One or more samples in list could not be displayed because you do not have enough rights to view them",
    "no_specimens": "There are no samples to show in selected sample list.",
    "remove_specimens": "Removing Samples From {{name}} list",
    "confirm_remove_specimens": "Are you sure you want to remove selected samples from {{name}} list?",
    "specimens_added": "Samples sucessfully added to {{name}} list",
    "specimens_removed": "Samples successfully removed from {{name}} list",
    "no_specimens_for_deletion": "Please select at least one sample for deletion",
    "no_specimens_for_distribution": "Please select at least one sample to create distribution order"
  },

  "specimens": { 
    "label": "Sample ID",
    "type": "Material type",
    "created_on": "Sampled time",
    "title": "Sample",
    "list": "Samples",
    "anticipated_list": "Anticipated Samples",
    "parent_specimen": "Parent Sample",
    "no_specimens_to_show": "No Samples to Show",
    "close_parent_q": "Do you want to close parent sample?",
    "more_info": "More Sample Information",
    "less_info": "Less Sample Information",
    "no_specimens_for_collection": "Please select at least one anticipated sample for collection",
    "no_specimens_for_print": "Please select at least one collected sample for label printing",
    "no_specimens_for_specimen_list": "Please select at least one sample to add sample list",
    "no_specimens_for_delete":"Please select at least one collected sample to delete",
    "no_specimens_for_close":"Please select at least one collected sample to close",
    "pos_selector": "Sample Position Selector",
    "labels_print_job_created": "Sample labels print job {{jobId}} created successfully",
    "bulk_import": "Bulk Import Samples",
    "spmn_extensions": "Sample Forms",
    "spmn_events": "Sample Events",
    "spmn_aliquots": "Sample Aliquots",
    "spmn_derivatives": "Sample Derivatives",
    "specimens_hierarchy_deleted": "Selected samples and their children are deleted successfully",
    "specimens_deleted": "Selected samples are deleted successfully",
    "delete_specimens_heirarchy": "Are you sure you want to delete selected samples and all its children ?",
    "delete_specimens":  "Are you sure you want to delete selected samples ?",
    "specimens_closed": "Selected samples are closed successfully",
    "specimen_closed": "Sample is closed successfully",
    "ctx_menu": {
      "view_specimen": "View Sample",
      "edit_specimen": "Edit Sample"
    },
    "errors": {
      "duplicate_labels": "One or more samples using same label",
      "insufficient_qty": "Insufficient parent sample quantity to create aliquots",
      "created_on_lt_parent": "Created on date/time of sample less than that of parent sample",
      "created_on_gt_curr_time": "Created on date/time of sample greater than current date/time"
    }
  },

  "visits": {
    "anticipated_specimens": "Anticipated Samples",
    "comments": "Description",
    "ctx_menu": {
      "collect_planned_specimens": "Collect Planned Samples",
      "collect_unplanned_specimens": "Collect Unplanned Samples",
      "print_specimen_labels": "Print Sample IDs"
    }
  },

  "srs": {
    "title": "Sample Requirement",
    "list": "Sample Requirements",
    "no_srs": "There are no sample requirements to show. Create a new requirement by clicking Add Sample Requirement ...",
    "new_sr": "New Sample Requirement",
    "specimen_class": "Sample Class",
    "specimen_type": "Material Type",
    "cannot_change_class_or_type": "Samples class or type cannot be changed once samples are collected",
    "deleting_sr": "Deleting Sample Requirement",
    "confirm_delete_sr": "Sample Requirement and all its children will be deleted. Are you sure you want to proceed?",
    "buttons": {
      "add_sr": "Add Sample Requirement..."
    }
  },

  "dp": {
    "tooltip": {
      "pending_count": "Pending to distribute samples count",
      "distributed_count": "Distributed samples count"
    }
  },

  "container": {
    "collection_protocol": "Study",
    "specimen_types": "Material Types",
    "cannot_hold_specimen": "Selected container cannot hold sample for which position is being selected",
    "stores_specimens": "Stores Samples",
    "paste_specimen_labels": "Paste or input sample labels separated by comma/tab/newline",
    "no_free_locs": "Container does not have enough free locations to accommodate input sample IDs",
    "no_containers_match_search": "No containers match search criteria to store sample/s"
  },

  "extensions": {
    "bulk_import_specimen_extns": "Bulk Import Sample Forms",
    "bulk_import_specimen_events": "Bulk Import Sample Events"
  },

  "queries": {
    "specimens": "Samples",
    "waiting_for_count": "Please wait for a moment while we count Participants and Samples matching your criteria",
    "select_cp": "Select a Study"
  },
  
  "entities": {
    "specimen": "Sample",
    "specimen_event": "Sample Event",
    "specimen_list": "Sample List",
    "collection_protocol": "Study",
    "collection_protocol_event": "Study Event",
    "collection_protocol_registration": "Study Registration"
  }, 
  
  "bulk_imports": {
    "object_types": {
      "specimen": "Samples",
      "specimenAliquot": "Sample Aliquots",
      "specimenDerivative": "Derived Samples",
      "cpr": "Study Registrations"
    }
  },

  "orders": {
     "specimens": "Samples",
     "no_specimens_in_list": "No samples in order list to distribute. Add at least one sample",
     "enter_specimen_label": "Enter sample labels separated by comma/tab/newline",
     "specimens_not_found_or_no_access": "One or more samples could not be loaded either because they do not exists or you do not have",
     "spec": {
      "cp": "Study"
    }
  }
}   



