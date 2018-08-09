package com.krishagni.openspecimen.msk;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum MskError implements ErrorCode  {
	NO_MRN,

	PATIENT_NOT_FOUND,

	DB_URL_NOT_SPECIFIED,

	DB_USER_NOT_SPECIFIED,

	DB_PASSWD_NOT_SPECIFIED,
	
	PROC_DT_NE_PATH_DT,

	PROC_DT_LT_OP_DT,

	PATH_DT_LT_OP_DT,

	SPMN_COLL_DT_LT_PROC_DT,

	COLL_DT_LT_PROC_DT,

	ACC_DT_LT_COLL_DT,

	RECV_DT_LT_COLL_DT,

	RECV_DT_LT_ACC_DT,

	CRET_DT_LT_RECV_DT;

	@Override
	public String code() {
		return "MSK_" + name();
	}
}
