package com.krishagni.openspecimen.wcmc.error;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum EpicErrorCode implements ErrorCode {
	DB_URL_REQ,
	
	DB_USER_REQ,
	
	DB_PWD_REQ,
	
	JSON_MAPPING_REQ;

	@Override
	public String code() {
		return "EPIC_" + name();
	}

}
