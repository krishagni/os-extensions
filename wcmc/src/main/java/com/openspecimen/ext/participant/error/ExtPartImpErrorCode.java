package com.openspecimen.ext.participant.error;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum ExtPartImpErrorCode implements ErrorCode {
	VAL_NOT_MAPPED;

	@Override
	public String code() {
		return "STAGING_IMPORTER_" + name();
	}
}
