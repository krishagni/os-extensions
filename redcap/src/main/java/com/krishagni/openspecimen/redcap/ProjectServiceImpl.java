package com.krishagni.openspecimen.redcap;

import java.io.File;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.openspecimen.redcap.ProjectAuditLog.Operation;
import com.krishagni.openspecimen.redcap.ProjectAuditLog.Status;
import com.krishagni.openspecimen.redcap.crf.Instrument;

public class ProjectServiceImpl implements ProjectService {
	private ProjectFactory projectFactory;
	
	private ProjectDao projectDao;

	private ProjectAuditLogDao projectAuditLogDao;
	
	private PlatformTransactionManager transactionManager;

	private ThreadPoolTaskExecutor taskExecutor;

	public void setProjectFactory(ProjectFactory projectFactory) {
		this.projectFactory = projectFactory;
	}

	public void setProjectDao(ProjectDao projectDao) {
		this.projectDao = projectDao;
	}

	public void setProjectAuditLogDao(ProjectAuditLogDao projectAuditLogDao) {
		this.projectAuditLogDao = projectAuditLogDao;
	}
	
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
	
	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@PlusTransactional
	@Override
	public ResponseEvent<List<ProjectDetail>> getProjects(RequestEvent<Long> req) {
		List<Project> projects = projectDao.getProjectsByCp(req.getPayload());
		return ResponseEvent.response(ProjectDetail.from(projects));		
	}

	@PlusTransactional
	@Override
	public ResponseEvent<ProjectDetail> createProject(RequestEvent<ProjectDetail> req) {
		try {
			Project project = projectFactory.createProject(req.getPayload());			
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(project.getCollectionProtocol());
		
			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureUniqueProjectMapping(null, project, ose);
			ensureUniqueProjectName(null, project, ose);
			ose.checkAndThrow();
			
			projectDao.saveOrUpdate(project);
			return ResponseEvent.response(ProjectDetail.from(project));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@PlusTransactional
	@Override
	public ResponseEvent<ProjectDetail> updateProject(RequestEvent<ProjectDetail> req) {
		try {
			Project project = projectFactory.createProject(req.getPayload());
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(project.getCollectionProtocol());
		
			Project existing = projectDao.getById(project.getId());
			if (existing == null) {
				return ResponseEvent.userError(ProjectErrorCode.NOT_FOUND);
			}
			
			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureUniqueProjectMapping(existing, project, ose);
			ensureUniqueProjectName(existing, project, ose);
			ose.checkAndThrow();
			
			existing.update(project);
			return ResponseEvent.response(ProjectDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}		
	}		

	@Override
	@PlusTransactional
	public ResponseEvent<Void> updateInstruments(RequestEvent<UpdateInstrumentsOp> req) {
		try {
			UpdateInstrumentsOp op = req.getPayload();
			List<Project> projects = Collections.emptyList();
			
			if (op.getCpId() != null && op.getProjectId() == null) {
				projects = projectDao.getProjectsByCp(op.getCpId());
			} else if (op.getProjectId() != null) {
				Project proj = projectDao.getById(op.getProjectId());
				if (proj == null) {
					return ResponseEvent.userError(ProjectErrorCode.NOT_FOUND);
				}
				
				projects = Collections.singletonList(proj);
			} else {
				projects = projectDao.getProjects();
			}
			
			if (CollectionUtils.isNotEmpty(projects)) {
				taskExecutor.execute(new UpdateInstrumentsTask(AuthUtil.getCurrentUser(), projects, op.getInstrumentNames()));
			}
			
			return ResponseEvent.response(null);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Void> updateData(RequestEvent<UpdateDataOp> req) {
		try {
			UpdateDataOp op = req.getPayload();
			Date startTs = op.getStartTs();
			Date endTs = op.getEndTs();
			
			if (endTs == null) {
				endTs = Calendar.getInstance().getTime();
			}
			
			if (startTs != null && startTs.after(endTs)) {
				//
				// TODO:
				//
				throw new RuntimeException("start interval cannot be greater than end interval");
			}
			
			List<Project> projects = Collections.emptyList();
			if (op.getCpId() != null && op.getProjectId() == null) {
				projects = projectDao.getProjectsByCp(op.getCpId());
			} else if (op.getProjectId() != null) {
				Project proj = projectDao.getById(op.getProjectId());
				if (proj == null) {
					return ResponseEvent.userError(ProjectErrorCode.NOT_FOUND);
				}
				
				projects = Collections.singletonList(proj);				
			} else {
				projects = projectDao.getProjects();
			}
			
			if (CollectionUtils.isNotEmpty(projects)) {
				for (Project project : projects) {
					project.getCollectionProtocol().getShortTitle();
				}
				
				taskExecutor.execute(new UpdateDataTask(AuthUtil.getCurrentUser(), projects, startTs, endTs));
			}
			
			return ResponseEvent.response(null);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<ProjectAuditLogDetail>> getProjectAuditLogs(RequestEvent<Long> req) {
		Long cpId = req.getPayload();
		return ResponseEvent.response(ProjectAuditLogDetail.from(projectAuditLogDao.getAllByCp(cpId)));
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<File> getFailedEventsLogFile(RequestEvent<Long> req) {
		try {
			ProjectAuditLog log = projectAuditLogDao.getById(req.getPayload());
			String path = null;
			
			if (log != null) {
				path = log.getFailedEventsLogFilePath();
			}
						
			if (StringUtils.isBlank(path)) {
				return ResponseEvent.response(null);
			}
			
			return ResponseEvent.response(new File(path));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}	
		
	//
	// Mostly used for internal usage purpose
	//
	
	@Override
	@PlusTransactional
	public Collection<Project> getProjects() {
		return projectDao.getProjects();
	}

	@Override
	@PlusTransactional
	public boolean isSyncInProgress(Long projectId) {
		ProjectAuditLog log = projectAuditLogDao.getLatestAuditLog(projectId);
		if (log == null) {
			return false;
		}
		
		return log.isInProgress();
	}

	@Override
	@PlusTransactional
	public Date getLatestDataSyncTime(Long projectId) {
		return projectAuditLogDao.getLatestDataSyncTime(projectId);
	}	
	
	private void ensureUniqueProjectMapping(Project existingProj, Project newProj, OpenSpecimenException ose) {
		Long newProjId = newProj.getProjectId();
		String newHost = newProj.getHostUrl();
		
		if (existingProj != null && 
			existingProj.getProjectId().equals(newProjId) &&
			existingProj.getHostUrl().equals(newHost)) {
			return;
		}
		
		Project other = projectDao.getProjectByHost(newProjId, newHost);
		if (other != null) {
			ose.addError(ProjectErrorCode.DUP_PROJECT_MAPPING);
		}
	}
	
	private void ensureUniqueProjectName(Project existingProj, Project newProj, OpenSpecimenException ose) {
		String name = newProj.getName();
		if (existingProj != null && existingProj.getName().equals(name)) {
			return;
		}
		
		Long cpId = newProj.getCollectionProtocol().getId();
		Project other = projectDao.getProjectByCpAndName(cpId, name);
		if (other != null) {
			ose.addError(ProjectErrorCode.DUP_NAME);
		}
	}
	
	private ProjectAuditLog newMetadataSyncLog(Project proj) {
		ProjectAuditLog auditLog = newLog(proj, Operation.METADATA_SYNC);
		saveLog(auditLog);
		return auditLog;
	}
		
	private ProjectAuditLog newDataSyncLog(Project proj, Date intervalStart, Date intervalEnd) {
		ProjectAuditLog auditLog = newLog(proj, Operation.DATA_SYNC);
		auditLog.setIntervalStartTime(intervalStart);
		auditLog.setIntervalEndTime(intervalEnd);
		saveLog(auditLog);
		return auditLog;		
	}
	
	private ProjectAuditLog newLog(Project proj, Operation op) {
		ProjectAuditLog auditLog = new ProjectAuditLog();
		auditLog.setProject(proj);
		auditLog.setOperation(op);
		auditLog.setStartTime(Calendar.getInstance().getTime());
		auditLog.setEndTime(auditLog.getEndTime());
		auditLog.setUser(AuthUtil.getCurrentUser());
		auditLog.setStatus(Status.IN_PROGRESS);
		auditLog.setNoOfRecords(0);
		return auditLog;		
	}
	
	private ProjectAuditLog logSuccess(ProjectAuditLog auditLog, int recsCount) {
		if (auditLog == null) {
			return null;
		}
		
		auditLog.setStatus(Status.SUCCESS);
		auditLog.setEndTime(Calendar.getInstance().getTime());
		auditLog.setNoOfRecords(recsCount);
		
		saveLog(auditLog);		
		return auditLog;
	}
	
	private ProjectAuditLog logFailure(ProjectAuditLog auditLog) {
		if (auditLog == null) {
			return null;
		}
		
		auditLog.setStatus(Status.FAILED);
		auditLog.setEndTime(Calendar.getInstance().getTime());
		saveLog(auditLog);
		
		return auditLog;
	}
	
	@PlusTransactional
	private void throwErrorIfProjectSyncInProgress(Project proj) {
		ProjectAuditLog latest = projectAuditLogDao.getLatestAuditLog(proj.getId());
		if (latest != null && latest.isInProgress()) {
			// TODO:
			throw new RuntimeException("Project sync is in progress");
		}		
	}
	
	private void saveLog(final ProjectAuditLog log) {
		if (log == null) {
			return;
		}
		
		TransactionTemplate tmpl = new TransactionTemplate(transactionManager);
		tmpl.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		tmpl.execute(new TransactionCallback<Void>() {

			@Override
			public Void doInTransaction(TransactionStatus status) {
				projectAuditLogDao.saveOrUpdate(log);
				return null;
			}
		});
	}
	
	@PlusTransactional
	private Set<Instrument> updateProjectMetadata(Project project, Set<String> instrumentNames) {
		ProjectAuditLog auditLog = null;
		
		try {
			throwErrorIfProjectSyncInProgress(project);
			
			auditLog = newMetadataSyncLog(project);			
			Set<Instrument> updatedInstruments = project.updateInstruments(instrumentNames);			
			logSuccess(auditLog, updatedInstruments.size());
			return updatedInstruments;			
		} catch (Exception e) {
			logFailure(auditLog);
			e.printStackTrace();
		}
		
		return Collections.emptySet();
	}
	
	@PlusTransactional
	private int updateProjectData(Project proj, Date startTs, Date endTs) {
		ProjectAuditLog auditLog = null;
		
		try {
			throwErrorIfProjectSyncInProgress(proj);
			
			if (startTs == null) {
				startTs = getStartTs(proj);
			}
			
			if (endTs == null) {
				endTs = Calendar.getInstance().getTime();
			}
			
			if (startTs != null && startTs.after(endTs)) {
				throw new RuntimeException("Start time cannot be greater than end time");
			}
						
			auditLog = newDataSyncLog(proj, startTs, endTs);
			
			proj.loadDictionary();
						
			int processedEvents = 0;
			for (LogEvent event : proj.getEvents(startTs, endTs)) {
				try {
					processEvent(proj, event);
					++processedEvents;
				} catch (Exception e) {
					auditLog.failedEvent(event, e);
					System.err.println("Couldn't process event: " + event);					
				}			
			}
			
			logSuccess(auditLog, processedEvents);
			return processedEvents;
		} catch (Exception e) {			
			logFailure(auditLog);
		} finally {
			proj.unloadDictionary();
			
			if (auditLog != null) {
				auditLog.closeFailedEventsLog();
			}
		}
		
		return 0;
	}
	
	private Date getStartTs(Project proj) {
		Date startTs = null;
		
		Date latestSyncTime = getLatestDataSyncTime(proj.getId());
		if (latestSyncTime != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(latestSyncTime);
			cal.add(Calendar.MILLISECOND, 1000); // add one second
			startTs = cal.getTime();
		}
		
		return startTs;
	}
	
	private void processEvent(final Project project, final LogEvent event) {
		TransactionTemplate tmpl = new TransactionTemplate(transactionManager);
		tmpl.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		tmpl.execute(new TransactionCallback<Void>() {

			@Override
			public Void doInTransaction(TransactionStatus status) {
				project.processEvent(event);
				return null;
			}			
		});
	}
	
	private void setUserContext(User user) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, "secret", user.getAuthorities()));
	}
	
	private class UpdateInstrumentsTask implements Runnable {
		private User user;
		
		private List<Project> projects;
		
		private Set<String> instrumentNames;
		
		public UpdateInstrumentsTask(User user, List<Project> projects, Set<String> instrumentNames) {
			this.user = user;
			this.projects = projects;
			this.instrumentNames = instrumentNames;
			
		}

		@Override
		public void run() {
			try {
				setUserContext(user);
				
				for (Project project : projects) {
					updateProjectMetadata(project, instrumentNames);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}			
		}
	}
	
	private class UpdateDataTask implements Runnable {
		private User user;
		
		private List<Project> projects;
		
		private Date startTs;
		
		private Date endTs;
		
		public UpdateDataTask(User user, List<Project> projects, Date startTs, Date endTs) {
			this.user = user;
			this.projects = projects;
			this.startTs = startTs;
			this.endTs = endTs;
		}
		
		@Override
		public void run() {
			try {
				setUserContext(user);
				
				for (Project project : projects) {
					updateProjectData(project, startTs, endTs);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}