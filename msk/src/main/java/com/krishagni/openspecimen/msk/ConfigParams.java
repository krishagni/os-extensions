package com.krishagni.openspecimen.msk;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

public class ConfigParams {
	private static String MODULE   = "mskcc";

	private static String URL      = "consents_db_url";

	private static String USERNAME = "consents_db_username";

	private static String PASSWORD = "consents_db_password";
	
	private static String EXPORT_JOB_DB_URL = "consents_db_export_job_url";
	
	private static String EXPORT_JOB_DB_USERNAME = "consents_db_export_job_username";
	
	private static String EXPORT_JOB_DB_PASSWORD = "consents_db_export_job_password";
	
	private static String EXPORT_JOB_DB_DIR = "consents_db_export_job_dir";

	public static String getUrl() {
		return getValue(URL, MskError.DB_URL_NOT_SPECIFIED);
	}

	public static String getUsername() {
		return getValue(USERNAME, MskError.DB_USER_NOT_SPECIFIED);
	}

	public static String getPassword() {
		return getValue(PASSWORD, MskError.DB_PASSWD_NOT_SPECIFIED);
	}
	
	public static String getExportDBUrl() {
		return getValue(EXPORT_JOB_DB_URL, MskError.DB_EXPORT_JOB_URL_NOT_SPECIFIED);
	}
	
	public static String getExportDBUsername() {
		return getValue(EXPORT_JOB_DB_USERNAME, MskError.DB_EXPORT_JOB_USERNAME_NOT_SPECIFIED);
	}
	
	public static String getExportDBPassword() {
		return getValue(EXPORT_JOB_DB_PASSWORD, MskError.DB_EXPORT_JOB_PASSWORD_NOT_SPECIFIED);
	}
	
	public static String getExportDBDir() {
		return getValue(EXPORT_JOB_DB_DIR, MskError.DB_EXPORT_JOB_DIR_NOT_SPECIFIED);
	}

	private static String getValue(String propName, ErrorCode errorCode) {
		String value = ConfigUtil.getInstance().getStrSetting(MODULE, propName, null);
		return ensureNotBlank(value, errorCode);
	}

	private static String ensureNotBlank(String value, ErrorCode code) {
		if (StringUtils.isBlank(value)) {
			throw OpenSpecimenException.userError(code);
		}

		return value;
	}
}
