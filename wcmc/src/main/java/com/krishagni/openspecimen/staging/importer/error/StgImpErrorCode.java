package com.krishagni.openspecimen.staging.importer.error;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum StgImpErrorCode implements ErrorCode {
	DB_URL_REQ,

	DB_USER_REQ,

	DB_PWD_REQ,

	JSON_MAPPING_REQ,

	VAL_NOT_MAPPED;

	@Override
	public String code() {
		return "STAGING_IMPORTER_" + name();
	}
}
