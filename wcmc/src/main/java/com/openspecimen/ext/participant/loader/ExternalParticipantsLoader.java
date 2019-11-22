package com.openspecimen.ext.participant.loader;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.biospecimen.events.StagedParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.services.StagedParticipantService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.openspecimen.ext.participant.source.ExternalParticipantSource;

@Configurable
public class ExternalParticipantsLoader {
	private List<ExternalParticipantSource> sources = new ArrayList<>();

	@Autowired
	private StagedParticipantService participantSvc;

	public StagedParticipantService getParticipantSvc() {
		return participantSvc;
	}

	public void setParticipantSvc(StagedParticipantService participantSvc) {
		this.participantSvc = participantSvc;
	}

	public List<ExternalParticipantSource> getSources() {
		return sources;
	}

	public void addSource(ExternalParticipantSource source) {
		if (!exists(source)) {
			sources.add(source);
		}
	}

	private boolean exists(ExternalParticipantSource source) {
		return sources.stream()
			.map(ExternalParticipantSource::getName)
			.anyMatch(e -> e.equalsIgnoreCase(source.getName()));
	}

	public void loadParticipants() {
		sources.forEach(this::loadParticipants);
	}

	private void loadParticipants(ExternalParticipantSource source) {
		for (StagedParticipantDetail detail : source.getParticipants()) {
			participantSvc.saveOrUpdateParticipant(RequestEvent.wrap(detail));
		}
	}
}
