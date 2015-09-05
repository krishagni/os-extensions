package com.krishagni.openspecimen.sgh;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum SghErrorCode implements ErrorCode{
	
	INVALID_PARTICIPANT_COUNT,
	
	INVALID_TRID_COUNT,
	
	CANNOT_PRINT_PLANNED_TRID,
	
	INVALID_TRID_SPECIFIED;

	@Override
	public String code() {
		return "SGH_" + this.name();
	}
}
