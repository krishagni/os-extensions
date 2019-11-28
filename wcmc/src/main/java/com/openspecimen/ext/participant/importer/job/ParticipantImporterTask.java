package com.openspecimen.ext.participant.importer.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.openspecimen.ext.participant.loader.ExternalParticipantsLoader;
import com.openspecimen.ext.participant.source.impl.ExternalDbParticipants;

@Configurable
public class ParticipantImporterTask implements ScheduledTask {

	@Autowired
	private ExternalParticipantsLoader loader;

	@Override
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		loader.loadParticipants();
	}
}