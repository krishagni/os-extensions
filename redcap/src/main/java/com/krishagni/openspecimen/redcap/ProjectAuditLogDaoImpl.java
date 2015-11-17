package com.krishagni.openspecimen.redcap;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class ProjectAuditLogDaoImpl extends AbstractDao<ProjectAuditLog> implements ProjectAuditLogDao {
	
	@Override
	public Class<ProjectAuditLog> getType() {
		return ProjectAuditLog.class;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ProjectAuditLog getLatestAuditLog(Long projectId) {
		List<ProjectAuditLog> logs = getSessionFactory().getCurrentSession()
			.getNamedQuery(GET_PROJ_AUDIT_LOGS)
			.setLong("projectId", projectId)
			.setMaxResults(1)
			.list();
		
		return CollectionUtils.isEmpty(logs) ? null : logs.iterator().next();
	}
	
	@Override
	public Date getLatestDataSyncTime(Long projectId) {
		return (Date)getSessionFactory().getCurrentSession()
			.getNamedQuery(GET_PROJ_LATEST_DATA_SYNC_TIME)
			.setLong("projectId", projectId)
			.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ProjectAuditLog> getAllByCp(Long cpId) {
		return getSessionFactory().getCurrentSession()
			.getNamedQuery(GET_PROJ_AUDIT_LOGS_BY_CP)
			.setLong("cpId", cpId)
			.list();
	}
	
	private static final String FQN = ProjectAuditLog.class.getName();
	
	private static final String GET_PROJ_AUDIT_LOGS = FQN + ".getProjectLogs";
	
	private static final String GET_PROJ_LATEST_DATA_SYNC_TIME = FQN + ".getProjectLatestDataSyncTime";
	
	private static final String GET_PROJ_AUDIT_LOGS_BY_CP = FQN + ".getProjectLogsByCp";


}