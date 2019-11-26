package com.openspecimen.ext.participant.loader;

import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.biospecimen.events.StagedParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.services.StagedParticipantService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.openspecimen.ext.participant.source.ExternalParticipantSource;

@Configurable
public class ExternalParticipantsLoader {
	private List<ExternalParticipantSource> sources;

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

	public void setSources(List<ExternalParticipantSource> sources) {
		this.sources = sources;
	}

	public void loadParticipants() throws SQLException {
		for (ExternalParticipantSource source : sources) {
			loadParticipants(source);
			source.shutdown();
		}
	}

	private void loadParticipants(ExternalParticipantSource source) {
		boolean hasMore = true;
		while (hasMore) {
			List<StagedParticipantDetail> participants = source.getParticipants();
			for (StagedParticipantDetail detail : participants) {
				participantSvc.saveOrUpdateParticipant(RequestEvent.wrap(detail));
			}
			hasMore = (participants.size() == source.getMaxResults());
		}
	}
}
