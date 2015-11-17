package com.krishagni.openspecimen.redcap.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.openspecimen.redcap.domain.ProjectAuditLog;

public class ProjectAuditLogDetail {
	private Long id;
	
	private Long projectId;
	
	private String name;
	
	private String operation;

	private Date startTime;
	
	private Date endTime;
	
	private Date intervalStartTime;
	
	private Date intervalEndTime;
	
	private UserSummary user;
	
	private Integer noOfRecords;
	
	private String failedEventsLog;
	
	private String status;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
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

	public UserSummary getUser() {
		return user;
	}

	public void setUser(UserSummary user) {
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
	public static ProjectAuditLogDetail from(ProjectAuditLog auditLog) {
		ProjectAuditLogDetail result = new ProjectAuditLogDetail();
		result.setId(auditLog.getId());
		result.setProjectId(auditLog.getProject().getProjectId());
		result.setName(auditLog.getProject().getName());
		result.setOperation(auditLog.getOperation().name());
		result.setStartTime(auditLog.getStartTime());
		result.setEndTime(auditLog.getEndTime());
		result.setIntervalStartTime(auditLog.getIntervalStartTime());
		result.setIntervalEndTime(auditLog.getIntervalEndTime());
		result.setUser(UserSummary.from(auditLog.getUser()));
		result.setNoOfRecords(auditLog.getNoOfRecords());
		result.setFailedEventsLog(auditLog.getFailedEventsLog());
		result.setStatus(auditLog.getStatus().name());
		
		return result;
	}
	
	public static List<ProjectAuditLogDetail> from(Collection<ProjectAuditLog> auditLogs) {
		List<ProjectAuditLogDetail> result = new ArrayList<ProjectAuditLogDetail>();
		
		for (ProjectAuditLog auditLog : auditLogs) {
			result.add(from(auditLog));
		}
		
		return result;
	}
}