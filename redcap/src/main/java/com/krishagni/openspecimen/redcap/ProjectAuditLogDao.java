package com.krishagni.openspecimen.redcap;

import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.common.repository.Dao;

public interface ProjectAuditLogDao extends Dao<ProjectAuditLog> {
	public ProjectAuditLog getLatestAuditLog(Long projectId);
	
	public Date getLatestDataSyncTime(Long projectId);
	
	public List<ProjectAuditLog> getAllByCp(Long cpId);
}
