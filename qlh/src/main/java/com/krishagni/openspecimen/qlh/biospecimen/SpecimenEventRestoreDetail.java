package com.krishagni.openspecimen.qlh.biospecimen;

import java.util.Date;
import java.util.List;

public class SpecimenEventRestoreDetail {
	private List<Long> specimenIds;

	private String overwrittenBy;

	private Date restoreUntil;

	public List<Long> getSpecimenIds() {
		return specimenIds;
	}

	public void setSpecimenIds(List<Long> specimenIds) {
		this.specimenIds = specimenIds;
	}

	public String getOverwrittenBy() {
		return overwrittenBy;
	}

	public void setOverwrittenBy(String overwrittenBy) {
		this.overwrittenBy = overwrittenBy;
	}

	public Date getRestoreUntil() {
		return restoreUntil;
	}

	public void setRestoreUntil(Date restoreUntil) {
		this.restoreUntil = restoreUntil;
	}
}
