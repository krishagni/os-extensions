package com.krishagni.openspecimen.spr.events;



public class SprReport {

	private String specimenId;
	private String name;
	private String collectionDate;
	private String text;
	private String mrn;
	private String pathId;
	
	public String getSpecimenId() {
		return specimenId;
	}
	
	public void setSpecimenId(String specimenId) {
		this.specimenId = specimenId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getCollectionDate() {
		return collectionDate;
	}
	
	public void setCollectionDate(String collectionDate) {
		this.collectionDate = collectionDate;
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public String getMrn() {
		return mrn;
	}

	public void setMrn(String mrn) {
		this.mrn = mrn;
	}

	public String getPathId() {
		return pathId;
	}

	public void setPathId(String pathId) {
		this.pathId = pathId;
	}
	
}
