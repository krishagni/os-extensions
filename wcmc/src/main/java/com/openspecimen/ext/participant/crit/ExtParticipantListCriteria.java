package com.openspecimen.ext.participant.crit;

import java.util.Date;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class ExtParticipantListCriteria extends AbstractListCriteria<ExtParticipantListCriteria> {
	private Date lastRun;

	public ExtParticipantListCriteria lastRun(Date lastRun) {
		this.lastRun = lastRun;
		return self();
	}

	public Date lastRun() {
		return this.lastRun;
	}

	@Override
	public ExtParticipantListCriteria self() {
		return this;
	}
}
