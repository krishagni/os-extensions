package com.krishagni.openspecimen.staging.importer.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.biospecimen.events.StagedParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.services.StagedParticipantService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.openspecimen.staging.importer.InterfaceHandler;
import com.krishagni.openspecimen.staging.importer.util.StgImpJobUtil;

@Configurable
public class StagingParticipantImporter extends InterfaceHandler {

	@Autowired
	private StagedParticipantService participantSvc;

	private Date lastRun;

	public StagedParticipantService getParticipantSvc() {
		return participantSvc;
	}

	public void setParticipantSvc(StagedParticipantService participantSvc) {
		this.participantSvc = participantSvc;
	}

	public void importStagingParticipants() {
		setupHandler();
		lastRun = StgImpJobUtil.getInstance().getLastJobRun(JOB_NAME);
		formatSql(lastRun == null ? "" : "where lastUpdatedDate > ?");

		processStagedParticipants();
	}

	@Override
	public StagedParticipantDetail saveStagedParticipant(StagedParticipantDetail stagedParticipant) {
		return saveOrUpdateParticipant(stagedParticipant);
	}

	private StagedParticipantDetail saveOrUpdateParticipant(StagedParticipantDetail input) {
		ResponseEvent<StagedParticipantDetail> resp = participantSvc.saveOrUpdateParticipant(new RequestEvent<>(input));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@Override
	public List<Object> getSqlArgs() {
		List<Object> params = new ArrayList<>(); 
		if (lastRun != null) {
			params.add(lastRun);
		}

		return params;
	}

	private static final String JOB_NAME = "Staging Participant Importer";
}
