package com.openspecimen.ext.participant.loader;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.krishagni.catissueplus.core.biospecimen.events.StagedParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.services.StagedParticipantService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.openspecimen.ext.participant.crit.ExtParticipantListCriteria;
import com.openspecimen.ext.participant.source.ExternalParticipantSource;

public class ExternalParticipantsLoader {
	private static final Log logger = LogFactory.getLog(ExternalParticipantsLoader.class);

	private List<ExternalParticipantSource> sources;

	private StagedParticipantService participantSvc;

	public List<ExternalParticipantSource> getSources() {
		return sources;
	}

	public void setSources(List<ExternalParticipantSource> sources) {
		this.sources = sources;
	}

	public StagedParticipantService getParticipantSvc() {
		return participantSvc;
	}

	public void setParticipantSvc(StagedParticipantService participantSvc) {
		this.participantSvc = participantSvc;
	}

	public void loadParticipants() {
		sources.forEach(this::loadParticipants);
	}

	private void loadParticipants(ExternalParticipantSource source) {
		try {
			source.init();
			ExtParticipantListCriteria param = new ExtParticipantListCriteria().startAt(0).maxResults(25);
			boolean hasMore = true;

			while (hasMore) {
				List<StagedParticipantDetail> participants = source.getParticipants(param);
				saveOrUpdateParticipants(participants);
				hasMore = (participants.size() == param.maxResults());
			}
		} catch (Exception e) {
			logger.error("Error occured while loading participants for the source: " + source.getName(), e);
		} finally {
			source.cleanUp();
		}
	}

	private void saveOrUpdateParticipants(List<StagedParticipantDetail> participants) {
		participants.forEach(this::saveOrUpdateParticipant);
	}

	private void saveOrUpdateParticipant(StagedParticipantDetail participant) {
		participantSvc.saveOrUpdateParticipant(RequestEvent.wrap(participant));
	}
}
