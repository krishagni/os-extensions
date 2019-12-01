package com.openspecimen.ext.participant.importer.job;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.openspecimen.ext.participant.crit.ExtParticipantListCriteria;
import com.openspecimen.ext.participant.loader.ExternalParticipantsLoader;

@Configurable
public class ParticipantImporterTask implements ScheduledTask {

	@Autowired
	private ExternalParticipantsLoader loader;

	@Autowired
	private DaoFactory daoFactory;

	@Override
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		loader.setCriteria(getCriteria(jobRun.getScheduledJob()));
		loader.loadParticipants();
	}

	private ExtParticipantListCriteria getCriteria(ScheduledJob scheduledJob) {
		return new ExtParticipantListCriteria().lastRun(getLastJobRun(scheduledJob.getName()));
	}

	@PlusTransactional
 	public Date getLastJobRun(String name) {
 		ScheduledJob job = daoFactory.getScheduledJobDao().getJobByName(name);
 		// This does not fetch the lastRunOn in the 'job' object.
 		// (I found no mapping of this attribute (lastRunOn) in the 'ScheduledJob.hbm.xml' file)
 		List<Long> jobIds = Collections.singletonList(job.getId());

 		Map<Long, Date> jobLastRun = daoFactory.getScheduledJobDao().getJobsLastRunTime(jobIds);
 		// This returns the current running job's time, instead of the its lastRun time. 
 		return jobLastRun.getOrDefault(job.getId(), null);
 	}
}