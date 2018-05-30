package com.krishagni.openspecimen.msk;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum ConsentError implements ErrorCode  {
	NO_MRN,

	PATIENT_NOT_FOUND,

	DB_URL_NOT_SPECIFIED,

	DB_USER_NOT_SPECIFIED,

	DB_PASSWD_NOT_SPECIFIED;

	@Override
	public String code() {
		return "MSK_" + name();
	}
}
