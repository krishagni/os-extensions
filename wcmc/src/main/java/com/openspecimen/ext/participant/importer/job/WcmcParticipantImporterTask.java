package com.openspecimen.ext.participant.importer.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.openspecimen.ext.participant.loader.ExternalParticipantsLoader;
import com.openspecimen.ext.participant.source.impl.ExternalDbParticipants;

@Configurable
public class WcmcParticipantImporterTask implements ScheduledTask {

	@Autowired
	private ExternalParticipantsLoader loader;

	public ExternalParticipantsLoader getLoader() {
		return loader;
	}

	public void setLoader(ExternalParticipantsLoader loader) {
		this.loader = loader;
	}

	@Override
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		loader.loadParticipants();
	}
}