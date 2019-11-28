package com.openspecimen.ext.participant.source;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.events.StagedParticipantDetail;
import com.openspecimen.ext.participant.crit.ExtParticipantListCriteria;

public interface ExternalParticipantSource {
	String getName();

	void init();

	List<StagedParticipantDetail> getParticipants(ExtParticipantListCriteria param);

	void cleanUp();
}
