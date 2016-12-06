package com.krishagni.os.jhuepic;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum EpicErrorCode implements ErrorCode{
	API_DETAILS_EMPTY,
	
	MATHCING_SITE_NOT_FOUND,

	PV_NOT_MAPPED,

	API_CALL_FAILED;

	public String code() {
		return "JHU_EPIC_" + this.name();
	}

}
