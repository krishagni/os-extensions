package com.krishagni.openspecimen.redcap;

import java.util.List;

import com.krishagni.catissueplus.core.common.repository.Dao;

public interface ProjectDao extends Dao<Project> {
	public List<Project> getProjects();
	
	public List<Project> getProjectsByCp(Long cpId);
	
	public Project getProjectByCpAndName(Long cpId, String name);
	
	public Project getProjectByHost(Long projectId, String hostUrl);
}
