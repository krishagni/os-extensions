package com.openspecimen.ext.participant.loader;

import java.util.List;

import com.openspecimen.ext.participant.crit.ExtParticipantListCriteria;
import com.openspecimen.ext.participant.source.ExternalParticipantSource;

import com.krishagni.catissueplus.core.biospecimen.events.StagedParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.services.StagedParticipantService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.util.LogUtil;

public class ExternalParticipantsLoader {
	private static final LogUtil logger = LogUtil.getLogger(ExternalParticipantsLoader.class);

	private List<ExternalParticipantSource> sources;

	private StagedParticipantService participantSvc;

	private ExtParticipantListCriteria criteria;

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

	public ExtParticipantListCriteria getCriteria() {
		return criteria;
	}

	public void setCriteria(ExtParticipantListCriteria criteria) {
		this.criteria = criteria;
	}

	public void loadParticipants() {
		sources.forEach(this::loadParticipants);
	}

	private void loadParticipants(ExternalParticipantSource source) {
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			source.init();
			criteria.startAt(0).maxResults(25);
			boolean hasMore = true;

			while (hasMore) {
				List<StagedParticipantDetail> participants = source.getParticipants(criteria);
				saveOrUpdateParticipants(participants);
				hasMore = (participants.size() == criteria.maxResults());
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
