package com.krishagni.openspecimen.spr.events;


public class SprDetailCrit {

	private String mrn;
	private String pathId;
	private boolean doEncoding;
	
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
	
	public boolean isDoEncoding() {
		return doEncoding;
	}
	
	public void setDoEncoding(boolean doEncoding) {
		this.doEncoding = doEncoding;
	}
	
}
