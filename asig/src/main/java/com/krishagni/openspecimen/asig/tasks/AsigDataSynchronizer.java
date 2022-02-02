package com.krishagni.openspecimen.asig.tasks;

import java.util.Calendar;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.openspecimen.asig.service.AsigService;

@Configurable
public class AsigDataSynchronizer implements ScheduledTask {
	private static final Logger logger = Logger.getLogger(AsigDataSynchronizer.class);

	@Autowired
	private AsigService asigService;

	@Override
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		logger.info("Asig data synchronization started - " + Calendar.getInstance().getTime());
		asigService.updateData();
		logger.info("Asig data synchronization finished - " + Calendar.getInstance().getTime());
	}
}