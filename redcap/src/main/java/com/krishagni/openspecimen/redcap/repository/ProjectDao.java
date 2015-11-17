package com.krishagni.openspecimen.redcap.repository;

import java.util.List;

import com.krishagni.catissueplus.core.common.repository.Dao;
import com.krishagni.openspecimen.redcap.domain.Project;

public interface ProjectDao extends Dao<Project> {
	public List<Project> getProjects();
	
	public List<Project> getProjectsByCp(Long cpId);
	
	public Project getProjectByCpAndName(Long cpId, String name);
	
	public Project getProjectByHost(Long projectId, String hostUrl);
}
