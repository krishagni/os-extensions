package com.krishagni.openspecimen.redcap;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

public class ProjectAuditLog extends BaseEntity {
	public enum Operation {
		METADATA_SYNC,
		
		DATA_SYNC
	};
	
	public enum Status {
		SUCCESS,
		
		IN_PROGRESS,
		
		FAILED
	};

	private Project project;
	
	private Operation operation;
	
	private Date startTime;
	
	private Date endTime;
	
	private Date intervalStartTime;
	
	private Date intervalEndTime;
	
	private User user;
	
	private Integer noOfRecords;
	
	private String failedEventsLog;
	
	private PrintWriter failedEventsLogWriter;
	
	private Status status;

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;
	}

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

	public Date getIntervalStartTime() {
		return intervalStartTime;
	}

	public void setIntervalStartTime(Date intervalStartTime) {
		this.intervalStartTime = intervalStartTime;
	}

	public Date getIntervalEndTime() {
		return intervalEndTime;
	}

	public void setIntervalEndTime(Date intervalEndTime) {
		this.intervalEndTime = intervalEndTime;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Integer getNoOfRecords() {
		return noOfRecords;
	}

	public void setNoOfRecords(Integer noOfRecords) {
		this.noOfRecords = noOfRecords;
	}

	public String getFailedEventsLog() {
		return failedEventsLog;
	}

	public void setFailedEventsLog(String failedEventsLog) {
		this.failedEventsLog = failedEventsLog;
	}
	
	public String getFailedEventsLogFilePath() {
		if (StringUtils.isBlank(failedEventsLog)) {
			return null;
		}
		
		return getFailedEventsLogDir() + File.separator + failedEventsLog; 
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
	
	public boolean isInProgress() {
		return status == Status.IN_PROGRESS;
	}
	
	public void failedEvent(LogEvent event, Exception e) {
		openFailedEventsLog();
		
		Calendar cal = Calendar.getInstance();
		failedEventsLogWriter.printf("%tF %tT|%s|%s\n", cal, cal, event.toString(), e.getMessage());
		failedEventsLogWriter.flush();
	}
	
	public void openFailedEventsLog() {
		if (failedEventsLogWriter != null) {
			return;
		}

		String dirPath = getFailedEventsLogDir();
		File dir = new File(dirPath);
		if (!dir.exists()) {
			dir.mkdirs();			
		}
		
		boolean newLog = StringUtils.isBlank(failedEventsLog);
		if (newLog) {
			Calendar cal = Calendar.getInstance();
			failedEventsLog = String.format(
				"failed_events_%tY%tm%td_%tH%tM_%d_%d.log", 
				cal, cal, cal, cal, cal, getProject().getId(), getId());
		}
		
		try {
			String logPath = dirPath + File.separator + failedEventsLog;			
			failedEventsLogWriter = new PrintWriter(new FileWriter(logPath, true));
		} catch (Exception e) {
			if (newLog) {
				failedEventsLog = null;
			}
			
			throw new RuntimeException("Failed to open failed events log file", e);
		}
	}
	
	public void closeFailedEventsLog() {		
		IOUtils.closeQuietly(failedEventsLogWriter);
	}
	
	private String getFailedEventsLogDir() {
		return ConfigUtil.getInstance().getDataDir() + File.separator + "redcap" + File.separator + "logs";
	}
}