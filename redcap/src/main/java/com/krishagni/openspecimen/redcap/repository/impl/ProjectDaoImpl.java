package com.krishagni.openspecimen.redcap.repository.impl;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.openspecimen.redcap.domain.Project;
import com.krishagni.openspecimen.redcap.repository.ProjectDao;

public class ProjectDaoImpl extends AbstractDao<Project> implements ProjectDao {
	
	@Override
	public Class<Project> getType() {
		return Project.class;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Project> getProjects() {
		return sessionFactory.getCurrentSession()
			.getNamedQuery(GET_PROJS)
			.list();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Project> getProjectsByCp(Long cpId) {
		return sessionFactory.getCurrentSession()
			.getNamedQuery(GET_PROJS_BY_CP)
			.setLong("cpId", cpId)
			.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Project getProjectByHost(Long projectId, String hostUrl) {
		List<Project> projects = sessionFactory.getCurrentSession()
			.getNamedQuery(GET_PROJ_BY_HOST)
			.setLong("projectId", projectId)
			.setString("hostUrl", hostUrl)
			.list();
		
		return CollectionUtils.isEmpty(projects) ? null : projects.iterator().next();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Project getProjectByCpAndName(Long cpId, String name) {
		List<Project> projects = sessionFactory.getCurrentSession()
			.getNamedQuery(GET_PROJ_BY_CP_AND_NAME)
			.setLong("cpId", cpId)
			.setString("projectName", name)
			.list();
		
		return CollectionUtils.isEmpty(projects) ? null : projects.iterator().next();
	}	
	
	private static final String FQN = Project.class.getName();
	
	private static final String GET_PROJS = FQN + ".getProjects";
	
	private static final String GET_PROJS_BY_CP = FQN + ".getProjectsByCp";
	
	private static final String GET_PROJ_BY_HOST = FQN + ".getProjectByHost";
	
	private static final String GET_PROJ_BY_CP_AND_NAME = FQN + ".getProjectByCpAndName";
}