package com.krishagni.openspecimen.redcap;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public interface ProjectService {
	public ResponseEvent<List<ProjectDetail>> getProjects(RequestEvent<Long> req);
	
	public ResponseEvent<ProjectDetail> createProject(RequestEvent<ProjectDetail> req);
	
	public ResponseEvent<ProjectDetail> updateProject(RequestEvent<ProjectDetail> req);
	
	public ResponseEvent<Void> updateInstruments(RequestEvent<UpdateInstrumentsOp> req);
	
	public ResponseEvent<Void> updateData(RequestEvent<UpdateDataOp> req);
	
	public ResponseEvent<List<ProjectAuditLogDetail>> getProjectAuditLogs(RequestEvent<Long> req);
	
	public ResponseEvent<File> getFailedEventsLogFile(RequestEvent<Long> req);
	
	//
	// Internal APIs
	//
	public Collection<Project> getProjects();
	
	public boolean isSyncInProgress(Long projectId);
	
	public Date getLatestDataSyncTime(Long projectId);
}
