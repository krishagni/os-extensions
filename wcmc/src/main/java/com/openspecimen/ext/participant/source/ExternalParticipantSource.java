package com.openspecimen.ext.participant.source;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.events.StagedParticipantDetail;

public interface ExternalParticipantSource {
	List<StagedParticipantDetail> getParticipants();

	String getName();
}
