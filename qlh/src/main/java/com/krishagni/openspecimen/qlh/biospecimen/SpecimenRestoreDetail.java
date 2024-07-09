package com.krishagni.openspecimen.qlh.biospecimen;

import java.util.Date;

public class SpecimenRestoreDetail {
	private Date from;

	private Date to;

	private String overwrittenBy;

	public Date getFrom() {
		return from;
	}

	public void setFrom(Date from) {
		this.from = from;
	}

	public Date getTo() {
		return to;
	}

	public void setTo(Date to) {
		this.to = to;
	}

	public String getOverwrittenBy() {
		return overwrittenBy;
	}

	public void setOverwrittenBy(String overwrittenBy) {
		this.overwrittenBy = overwrittenBy;
	}
}
