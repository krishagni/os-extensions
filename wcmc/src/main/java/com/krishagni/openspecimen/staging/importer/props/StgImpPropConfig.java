package com.krishagni.openspecimen.staging.importer.props;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.openspecimen.staging.importer.error.StgImpErrorCode;

public class StgImpPropConfig {
	private static StgImpPropConfig instance;
	
	public static StgImpPropConfig getInstance() {
		if (instance == null) {
			instance = new StgImpPropConfig();
		}
		return instance;
	}
	
	public String getDbUrl() {
		return getConfigSetting(DB_URL, StgImpErrorCode.DB_URL_REQ);
	}
	
	public String getDbUser() {
		return getConfigSetting(DB_USER, StgImpErrorCode.DB_USER_REQ);
	}
	
	public String getDbPwd() {
		return getConfigSetting(DB_PWD, StgImpErrorCode.DB_PWD_REQ);
	}
	
	public String getJsonMapping() {
		return getConfigSetting(JSON_MAPPING, StgImpErrorCode.JSON_MAPPING_REQ);
	}

	private String getConfigSetting(String name, ErrorCode errorCode) {
		String result = ConfigUtil.getInstance().getStrSetting(MODULE, name, null);
		if (StringUtils.isBlank(result)) {
			throw OpenSpecimenException.userError(errorCode);
		}

		return result;
	}

	private static final String MODULE = "staging_importer";
	
	private static final String DB_URL = "stg_imp_db_url";
	
	private static final String DB_USER = "stg_imp_db_user";
	
	private static final String DB_PWD = "stg_imp_db_pwd";
	
	private static final String JSON_MAPPING = "stg_imp_json_mapping";
}