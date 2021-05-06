package com.krishagni.openspecimen.mcrigenv;

import java.util.Date;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;

public class SpecimenNotif extends BaseEntity {
	private Specimen specimen;

	private PermissibleValue receiveQuality;

	private Date receiveQualityNotifTime;

	private Boolean missed;

	private Date missedNotifTime;

	public Specimen getSpecimen() {
		return specimen;
	}

	public void setSpecimen(Specimen specimen) {
		this.specimen = specimen;
	}

	public PermissibleValue getReceiveQuality() {
		return receiveQuality;
	}

	public void setReceiveQuality(PermissibleValue receiveQuality) {
		this.receiveQuality = receiveQuality;
	}

	public Date getReceiveQualityNotifTime() {
		return receiveQualityNotifTime;
	}

	public void setReceiveQualityNotifTime(Date receiveQualityNotifTime) {
		this.receiveQualityNotifTime = receiveQualityNotifTime;
	}

	public Boolean getMissed() {
		return missed;
	}

	public void setMissed(Boolean missed) {
		this.missed = missed;
	}

	public Date getMissedNotifTime() {
		return missedNotifTime;
	}

	public void setMissedNotifTime(Date missedNotifTime) {
		this.missedNotifTime = missedNotifTime;
	}
}
