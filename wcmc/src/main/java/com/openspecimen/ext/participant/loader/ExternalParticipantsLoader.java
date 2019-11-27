package com.openspecimen.ext.participant.loader;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.biospecimen.events.StagedParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.services.StagedParticipantService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.openspecimen.ext.participant.crit.ExtParticipantListCriteria;
import com.openspecimen.ext.participant.source.ExternalParticipantSource;

@Configurable
public class ExternalParticipantsLoader {
	private static final Log logger = LogFactory.getLog(ExternalParticipantsLoader.class);

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
				for (StagedParticipantDetail detail : participants) {
					participantSvc.saveOrUpdateParticipant(RequestEvent.wrap(detail));
				}
				hasMore = (participants.size() == param.maxResults());
			}
		} catch (Exception e) {
			logger.error("Error occured while loading participants for the source: " + source.getName(), e);
		} finally {
			source.cleanUp();
		}
	}
}
