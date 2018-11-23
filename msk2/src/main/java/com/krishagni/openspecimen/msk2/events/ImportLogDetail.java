package com.krishagni.openspecimen.msk2.events;

import org.apache.commons.lang3.StringUtils;

public class ImportLogDetail {
	private String error = "";

	private boolean updated;

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public boolean isErroneous() {
		return StringUtils.isNotBlank(error);
	}

	public boolean isUpdated() {
		return updated;
	}

	public void setUpdated(boolean updated) {
		this.updated = updated;
	}
}
