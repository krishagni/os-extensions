package com.krishagni.openspecimen.redcap.domain;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum ProjectErrorCode implements ErrorCode {
	NOT_FOUND,
	
	NAME_REQ,
	
	HOST_URL_REQ,

	PROJECT_ID_REQ,
	
	API_TOKEN_REQ,
	
	INVALID_TRANSFORMER_FQN,
	
	SUBJECT_FIELDS_MAPPING_REQ,
	
	VISIT_FIELDS_MAPPING_REQ,
	
	CP_REQ,
	
	DUP_PROJECT_MAPPING,
	
	DUP_NAME,
	
	CPR_NOT_FOUND,
	
	VISIT_NOT_FOUND;

	@Override
	public String code() {		
		return "RC_PROJ_" + this.name();
	}
}
