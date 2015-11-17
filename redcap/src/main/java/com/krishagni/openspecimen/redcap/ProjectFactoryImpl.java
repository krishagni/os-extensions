package com.krishagni.openspecimen.redcap;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Status;

public class ProjectFactoryImpl implements ProjectFactory {
	
	private static final String regex = "[\\t|\\r|\\n|\\s|,]+";
	
	private DaoFactory daoFactory;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public Project createProject(ProjectDetail detail) {
		Project project = new Project();
		
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		project.setId(detail.getId());
		setName(detail, project, ose);
		setHostUrl(detail, project, ose);
		setProjectId(detail, project, ose);
		setApiToken(detail, project, ose);
		setTransformerFqn(detail, project, ose);
		setSubjectFields(detail, project, ose);
		setVisitFields(detail, project, ose);
		setCollectionProtocol(detail, project, ose);
		
		project.setUpdatedBy(AuthUtil.getCurrentUser());
		project.setUpdateTime(Calendar.getInstance().getTime());
		setActivityStatus(detail, project, ose);
		
		ose.checkAndThrow();
		return project;
	}
	
	private void setName(ProjectDetail detail, Project project, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getName())) {
			ose.addError(ProjectErrorCode.NAME_REQ);
			return;
		}
		
		project.setName(detail.getName());
	}
	
	private void setHostUrl(ProjectDetail detail, Project project, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getHostUrl())) {
			ose.addError(ProjectErrorCode.HOST_URL_REQ);
			return;
		}
		
		StringBuilder url = new StringBuilder(detail.getHostUrl());
		for (int i = url.length() - 1; url.charAt(i) == '/'; --i) {
			url.deleteCharAt(i);
		}
		
		project.setHostUrl(url.toString());
	}
	
	
	private void setProjectId(ProjectDetail detail, Project project, OpenSpecimenException ose) {
		if (detail.getProjectId() == null) {
			ose.addError(ProjectErrorCode.PROJECT_ID_REQ);
			return;
		}
		
		project.setProjectId(detail.getProjectId());
	}

	private void setApiToken(ProjectDetail detail, Project project, OpenSpecimenException ose) {
		if (detail.getId() == null && StringUtils.isBlank(detail.getApiToken())) {
			ose.addError(ProjectErrorCode.API_TOKEN_REQ);
			return;
		}
		
		project.setApiToken(detail.getApiToken());
	}
	
	private void setTransformerFqn(ProjectDetail detail, Project project, OpenSpecimenException ose) {
		String fqn = detail.getTransformerFqn();
		if (StringUtils.isBlank(fqn)) {
			return;
		}
		
		try {
			Class.forName(fqn);
			project.setTransformerFqns(Collections.singletonList(fqn));
		} catch (Exception e) {
			ose.addError(ProjectErrorCode.INVALID_TRANSFORMER_FQN);
		}
	}
	
	private void setSubjectFields(ProjectDetail detail, Project project, OpenSpecimenException ose) {
		project.setSubjectFields(getMap(detail.getSubjectFields(), ose, ProjectErrorCode.SUBJECT_FIELDS_MAPPING_REQ));
	}
	
	private void setVisitFields(ProjectDetail detail, Project project, OpenSpecimenException ose) {
		project.setVisitFields(getMap(detail.getVisitFields(), ose, ProjectErrorCode.VISIT_FIELDS_MAPPING_REQ));
	}
	
	private void setActivityStatus(ProjectDetail detail, Project project, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getActivityStatus())) {
			project.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
			return;
		}
		
		if (!Status.isValidActivityStatus(detail.getActivityStatus())) {
			ose.addError(ActivityStatusErrorCode.INVALID);
			return;			
		}
		
		project.setActivityStatus(detail.getActivityStatus());
	}
		
	private void setCollectionProtocol(ProjectDetail detail, Project project, OpenSpecimenException ose) {
		Long cpId = detail.getCpId();
		if (cpId == null) {
			ose.addError(ProjectErrorCode.CP_REQ);
			return;
		}
		
		CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(cpId);
		if (cp == null) {
			ose.addError(CpErrorCode.NOT_FOUND);
			return;
		}
		
		project.setCollectionProtocol(cp);
	}
	
	private Map<String, String> getMap(String input, OpenSpecimenException ose, ProjectErrorCode error) {		
		if (StringUtils.isBlank(input)) {
			ose.addError(error);
			return null;
		}
		
		String[] kvPairs = input.split(regex);
		Map<String, String> map = new HashMap<String, String>();
		for (String kvPair : kvPairs) {
			String[] kv = kvPair.split("=");
			map.put(kv[0], kv[1]);
		}
		
		return map;
	}	
}
