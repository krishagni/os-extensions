package com.krishagni.openspecimen.redcap.repository;

import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.common.repository.Dao;
import com.krishagni.openspecimen.redcap.domain.ProjectAuditLog;

public interface ProjectAuditLogDao extends Dao<ProjectAuditLog> {
	public ProjectAuditLog getLatestAuditLog(Long projectId);
	
	public Date getLatestDataSyncTime(Long projectId);
	
	public List<ProjectAuditLog> getAllByCp(Long cpId);
}
