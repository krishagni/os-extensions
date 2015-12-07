package com.krishagni.openspecimen.unsw.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum IdentifiedSprErrorCode implements ErrorCode {
	
	NOT_FOUND,
	
	FORM_NOT_FOUND;
	
	@Override
	public String code() {
		return "IDENTIFIED_SPR_" + this.name();
	}
}
