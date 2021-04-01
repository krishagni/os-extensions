package com.krishagni.openspecimen.mcrigenv.jms;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum JmsFreezerErrorCode implements ErrorCode {
	CONN_FACTORY_OR_Q_NS;

	@Override
	public String code() {
		return "MCRI_JMS" + name();
	}
}
