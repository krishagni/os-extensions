package com.krishagni.os.jhuepic;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum EpicErrorCode implements ErrorCode{

	API_DETAILS_CANNOT_EMPTY,
	
	MATHCING_SITE_NOT_FOUND,
	
	GENDER_MAPPING_NOT_FOUND,
	
	VITAL_STAT_MAPPING_NOT_FOUND,
	
	ETHNICITY_MAPPING_NOT_FOUND,
	
	RACE_MAPPING_NOT_FOUND;
	
	public String code() {
		return "JHU_EPIC_" + this.name();
	}

}
