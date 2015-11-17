package com.krishagni.openspecimen.redcap;

import java.util.Set;

public class UpdateInstrumentsOp {
	private Long cpId;
	
	private Long projectId;
	
	private Set<String> instrumentNames;

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public Set<String> getInstrumentNames() {
		return instrumentNames;
	}

	public void setInstrumentNames(Set<String> instrumentNames) {
		this.instrumentNames = instrumentNames;
	}
}