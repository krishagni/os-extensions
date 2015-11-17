package com.krishagni.openspecimen.redcap.domain.factory;

import com.krishagni.openspecimen.redcap.domain.Project;
import com.krishagni.openspecimen.redcap.events.ProjectDetail;

public interface ProjectFactory {
	public Project createProject(ProjectDetail projectDetail);
}
