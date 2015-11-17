package com.krishagni.openspecimen.redcap;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;

public class Record extends BaseEntity {
	private String recordId;
	
	private Project project;
	
	private Long cprId;
	
	private Long visitId;

	public String getRecordId() {
		return recordId;
	}

	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public Long getCprId() {
		return cprId;
	}

	public void setCprId(Long cprId) {
		this.cprId = cprId;
	}

	public Long getVisitId() {
		return visitId;
	}

	public void setVisitId(Long visitId) {
		this.visitId = visitId;
	}
}