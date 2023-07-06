package com.krishagni.openspecimen.uams;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum EpicErrorCode implements ErrorCode {
	API_DETAILS_EMPTY,

	PV_NOT_MAPPED,

	API_CALL_FAILED;

	public String code() {
		return "UAMS_EPIC_" + this.name();
	}

}