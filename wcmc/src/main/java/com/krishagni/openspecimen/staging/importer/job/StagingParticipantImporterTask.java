package com.krishagni.openspecimen.staging.importer.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;

@Configurable
public class StagingParticipantImporterTask implements ScheduledTask {

	@Autowired
	private StagingParticipantImporter importer;

	public StagingParticipantImporter getImporter() {
		return importer;
	}

	public void setImporter(StagingParticipantImporter importer) {
		this.importer = importer;
	}

	@Override
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		importer.importStagingParticipants();
	}
}
