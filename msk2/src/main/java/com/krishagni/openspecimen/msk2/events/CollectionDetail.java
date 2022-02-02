package com.krishagni.openspecimen.msk2.events;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

public class CollectionDetail extends ImportLogDetail {
	private static final String TISSUE = "Tissue";

	private static final String TISSUE_T = "Tissue (T)";

	private String id;

	private String name;

	private String type;

	private String container;

	private Date creationTime;

	private String comments;

	private int processed;

	private int shipped;

	private Date updateTime;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = TISSUE.equals(type) ? TISSUE_T : type;
	}

	public String getContainer() {
		return container;
	}

	public void setContainer(String container) {
		this.container = container;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public int getProcessed() {
		return processed;
	}

	public void setProcessed(int processed) {
		this.processed = processed;
	}

	public int getShipped() {
		return shipped;
	}

	public void setShipped(int shipped) {
		this.shipped = shipped;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}
}
