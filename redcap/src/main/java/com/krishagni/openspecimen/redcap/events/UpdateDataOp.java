package com.krishagni.openspecimen.redcap.events;

import java.util.Date;

public class UpdateDataOp {
	private Long cpId;
	
	private Long projectId;
	
	private Date startTs;
	
	private Date endTs;

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

	public Date getStartTs() {
		return startTs;
	}

	public void setStartTs(Date startTs) {
		this.startTs = startTs;
	}

	public Date getEndTs() {
		return endTs;
	}

	public void setEndTs(Date endTs) {
		this.endTs = endTs;
	}

}
