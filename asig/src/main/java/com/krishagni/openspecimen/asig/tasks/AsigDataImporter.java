package com.krishagni.openspecimen.asig.tasks;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.openspecimen.asig.service.AsigDataImporterService;

@Configurable
public class AsigDataImporter implements ScheduledTask {
	private static final Logger logger = Logger.getLogger(AsigDataImporter.class);

	@Autowired
	private AsigDataImporterService asigService;

	@Override
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		logger.info("Loading Asig data into staging tables");
		asigService.importAsigData();
		logger.info("Asig data loading completed successfully");
	}
}