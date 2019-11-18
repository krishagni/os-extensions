package com.krishagni.openspecimen.staging.importer.util;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;

@Configurable
public class StgImpJobUtil {
	private static StgImpJobUtil instance;

	@Autowired
	private DaoFactory daoFactory;

	public DaoFactory getDaoFactory() {
		return daoFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public static StgImpJobUtil getInstance() {
		if (instance == null) {
			instance = new StgImpJobUtil();
		}
		return instance;
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
