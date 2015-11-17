package com.krishagni.openspecimen.redcap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.krishagni.catissueplus.core.common.events.UserSummary;

public class ProjectDetail {
	private Long id;
	
	private String name;
	
	private String hostUrl;
	
	private Long projectId;
	
	private String apiToken;
	
	private String transformerFqn;
	
	private String subjectFields;
	
	private String visitFields;
	
	private Long cpId;
	
	private UserSummary updatedBy;
	
	private Date updateTime;
	
	private String activityStatus;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHostUrl() {
		return hostUrl;
	}

	public void setHostUrl(String hostUrl) {
		this.hostUrl = hostUrl;
	}

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public String getApiToken() {
		return apiToken;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}

	public String getTransformerFqn() {
		return transformerFqn;
	}

	public void setTransformerFqn(String transformerFqn) {
		this.transformerFqn = transformerFqn;
	}

	public String getSubjectFields() {
		return subjectFields;
	}

	public void setSubjectFields(String subjectFields) {
		this.subjectFields = subjectFields;
	}

	public String getVisitFields() {
		return visitFields;
	}

	public void setVisitFields(String visitFields) {
		this.visitFields = visitFields;
	}
	
	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public UserSummary getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(UserSummary updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public static ProjectDetail from(Project project) {
		ProjectDetail result = new ProjectDetail();
		result.setId(project.getId());
		result.setName(project.getName());
		result.setHostUrl(project.getHostUrl());
		result.setProjectId(project.getProjectId());
		//result.setApiToken(project.getApiToken());
		
		List<String> fqns = project.getTransformerFqns();
		if (CollectionUtils.isNotEmpty(fqns)) {
			result.setTransformerFqn(fqns.iterator().next());
		}
		
		String kv = project.getSubjectFields().toString();
		result.setSubjectFields(kv.substring(1, kv.length() - 1));
		
		kv = project.getVisitFields().toString();
		result.setVisitFields(kv.substring(1, kv.length() - 1));
		
		result.setCpId(project.getCollectionProtocol().getId());
		result.setUpdatedBy(UserSummary.from(project.getUpdatedBy()));
		result.setUpdateTime(project.getUpdateTime());
		result.setActivityStatus(project.getActivityStatus());
		return result;
	}
	
	public static List<ProjectDetail> from(Collection<Project> projects) {
		List<ProjectDetail> results = new ArrayList<ProjectDetail>();
		
		for (Project project : projects) {
			results.add(ProjectDetail.from(project));
		}
		
		return results;
	} 
}
