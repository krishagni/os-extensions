package com.krishagni.openspecimen.unsw.events;

import java.util.Map;

public class IdentifiedSprDetail {

	private Long formId;
	
	private Long formContextId;
	
	private Long recordId;
	
	private Map<String, Object> formData; 

	public Long getFormId() {
		return formId;
	}

	public void setFormId(Long formId) {
		this.formId = formId;
	}

	public Long getFormContextId() {
		return formContextId;
	}

	public void setFormContextId(Long formContextId) {
		this.formContextId = formContextId;
	}

	public Long getRecordId() {
		return recordId;
	}

	public void setRecordId(Long recordId) {
		this.recordId = recordId;
	}

	public Map<String, Object> getFormData() {
		return formData;
	}

	public void setFormData(Map<String, Object> formData) {
		this.formData = formData;
	}
}
