package com.krishagni.openspecimen.msk2.domain;

import java.io.File;
import java.util.Date;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;

public class CarsStudyImportJob extends BaseEntity {
	private Date startTime;

	private Date endTime;

	private int noOfStudies;

	private int failedStudies;

	private File logsFile;

	private User runBy;

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public int getNoOfStudies() {
		return noOfStudies;
	}

	public void setNoOfStudies(int noOfStudies) {
		this.noOfStudies = noOfStudies;
	}

	public int getFailedStudies() {
		return failedStudies;
	}

	public void setFailedStudies(int failedStudies) {
		this.failedStudies = failedStudies;
	}

	public File getLogsFile() {
		return logsFile;
	}

	public void setLogsFile(File logsFile) {
		this.logsFile = logsFile;
	}

	public User getRunBy() {
		return runBy;
	}

	public void setRunBy(User runBy) {
		this.runBy = runBy;
	}
}
