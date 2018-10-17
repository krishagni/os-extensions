package com.krishagni.openspecimen.msk2.events;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TimepointDetail {
	private String id;

	private String name;

	private String cycle;

	private Date creationTime;

	private Date updateTime;

	private List<CollectionDetail> collections = new ArrayList<>();

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

	public String getCycle() {
		return cycle;
	}

	public void setCycle(String cycle) {
		this.cycle = cycle;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public List<CollectionDetail> getCollections() {
		return collections;
	}

	public void setCollections(List<CollectionDetail> collections) {
		this.collections = collections;
	}

	public void addCollection(CollectionDetail collection) {
		collections.add(collection);
	}
}
