package com.krishagni.openspecimen.usb.error;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum PrintTokenErrorCode implements ErrorCode {
	INVALID_LIS_GERM_CODE_UDN;

	@Override
	public String code() {
		return "usb_" + name();
	}

}
