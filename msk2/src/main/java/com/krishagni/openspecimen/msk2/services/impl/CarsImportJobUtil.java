package com.krishagni.openspecimen.msk2.services.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.importer.domain.ImportJob;
import com.krishagni.catissueplus.core.importer.repository.ImportJobDao;
import com.krishagni.catissueplus.core.importer.repository.ListImportJobsCriteria;

@Configurable
public class CarsImportJobUtil {
	private static CarsImportJobUtil instance;

	@Autowired
	private ImportJobDao importJobDao;

	public static CarsImportJobUtil getInstance() {
		if (instance == null) {
			instance = new CarsImportJobUtil();
		}
		return instance;
	}

	@PlusTransactional
	public ImportJob createJob(String name) {
		ImportJob job = new ImportJob();
		job.setName(name);
		job.setAtomic(false);
		job.setCreatedBy(AuthUtil.getCurrentUser());
		job.setCreationTime(Calendar.getInstance().getTime());
		job.setCsvtype(ImportJob.CsvType.SINGLE_ROW_PER_OBJ);
		job.setStatus(ImportJob.Status.IN_PROGRESS);
		job.setType(ImportJob.Type.UPDATE);

		importJobDao.saveOrUpdate(job, true);
		return job;
	}

	@PlusTransactional
	public ImportJob getLatestJob(String name) {
		List<String> objectTypes = Collections.singletonList(name);
		ListImportJobsCriteria listCrit = new ListImportJobsCriteria().objectTypes(objectTypes).maxResults(1);
		List<ImportJob> jobs = importJobDao.getImportJobs(listCrit);
		return jobs.isEmpty() ? null : jobs.iterator().next();
	}

	@PlusTransactional
	public Date getLastUpdated(String name) {
		List<String> objectTypes = Collections.singletonList(name);
		ListImportJobsCriteria listCrit = new ListImportJobsCriteria().objectTypes(objectTypes);
		int startAt = 0;

		Date result = null;
		List<ImportJob> jobs = new ArrayList<>();
		while (true) {
			if (jobs.isEmpty()) {
				jobs = importJobDao.getImportJobs(listCrit.startAt(startAt));
				startAt += 100;
				if (jobs.isEmpty()) {
					break;
				}
			}

			result = getLastUpdated(jobs.remove(0));
			if (result != null) {
				break;
			}
		}

		return result;
	}

	public Date getLastUpdated(ImportJob lastJob) {
		String lastUpdated = lastJob.getParams().get("lastUpdated");
		if (StringUtils.isBlank(lastUpdated)) {
			return null;
		}

		try {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(Long.parseLong(lastUpdated));
			return cal.getTime();
		} catch (NumberFormatException nfe) {
			return null;
		}
	}

	@PlusTransactional
	public void finishJob(ImportJob job, int totalRecords, int failedRecords, Date lastUpdated) {
		job.setEndTime(Calendar.getInstance().getTime());
		job.setStatus(failedRecords > 0 ? ImportJob.Status.FAILED : ImportJob.Status.COMPLETED);
		job.setTotalRecords((long) totalRecords);
		job.setFailedRecords((long) failedRecords);
		if (lastUpdated != null) {
			job.setParams(Collections.singletonMap("lastUpdated", String.valueOf(lastUpdated.getTime())));
		}

		importJobDao.saveOrUpdate(job);
	}
}
