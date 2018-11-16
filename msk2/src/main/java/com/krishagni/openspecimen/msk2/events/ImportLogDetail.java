package com.krishagni.openspecimen.msk2.events;

import org.apache.commons.lang3.StringUtils;

public class ImportLogDetail {
	private String error = "";

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public boolean isErroneous() {
		return StringUtils.isNotBlank(error);
	}
}
