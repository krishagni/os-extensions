package com.krishagni.openspecimen.msk2.domain;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum CarsErrorCode implements ErrorCode {
	DB_URL_REQ,

	DB_USERNAME_REQ,

	DB_PASSWORD_REQ;

	@Override
	public String code() {
		return "MSK2_CARS_" + name();
	}
}
