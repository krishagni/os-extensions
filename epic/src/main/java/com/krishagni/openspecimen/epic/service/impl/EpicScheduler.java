package com.krishagni.openspecimen.epic.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.openspecimen.epic.service.EpicService;

@Configurable
public class EpicScheduler implements ScheduledTask{

	@Autowired
	private EpicService epicSvc;
	
	public void setEpicSvc(EpicService epicSvc) {
		this.epicSvc = epicSvc;
	}

	@Override
	public void doJob(ScheduledJobRun arg0) throws Exception {
		epicSvc.registerParticipants();
	}

}
