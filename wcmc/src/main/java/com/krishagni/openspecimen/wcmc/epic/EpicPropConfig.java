package com.krishagni.openspecimen.wcmc.epic;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.openspecimen.wcmc.error.EpicErrorCode;

public class EpicPropConfig {
	private static EpicPropConfig instance;
	
	public static EpicPropConfig getInstance() {
		if (instance == null) {
			instance = new EpicPropConfig();
		}
		return instance;
	}
	
	public String getDbUrl() {
		return getConfigSetting(DB_URL, EpicErrorCode.DB_URL_REQ);
	}
	
	public String getDbUser() {
		return getConfigSetting(DB_USER, EpicErrorCode.DB_USER_REQ);
	}
	
	public String getDbPwd() {
		return getConfigSetting(DB_PWD, EpicErrorCode.DB_PWD_REQ);
	}
	
	public String getJsonMapping() {
		return getConfigSetting(JSON_MAPPING, EpicErrorCode.JSON_MAPPING_REQ);
	}

	private String getConfigSetting(String name, ErrorCode errorCode) {
		String result = ConfigUtil.getInstance().getStrSetting(MODULE, name, null);
		if (StringUtils.isBlank(result)) {
			throw OpenSpecimenException.userError(errorCode);
		}

		return result;
	}

	private static final String MODULE = "epic_int";
	
	private static final String DB_URL = "epic_db_url";
	
	private static final String DB_USER = "epic_db_user";
	
	private static final String DB_PWD = "epic_db_pwd";
	
	private static final String JSON_MAPPING = "epic_json_mapping";
}