package com.krishagni.openspecimen.msk2.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.openspecimen.msk2.services.CarsStudyImporter;

@Configurable
public class CarsStudyImportTask implements ScheduledTask {

	@Autowired
	private CarsStudyImporter importer;

	@Override
	public void doJob(ScheduledJobRun jobRun)
	throws Exception {
		importer.importStudies();
	}
}
