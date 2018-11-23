package com.krishagni.openspecimen.msk2.events;

import java.util.ArrayList;
import java.util.List;

public class CarsStudyDetail extends ImportLogDetail {
	private String irbNumber;

	private String facility;

	private String piAddress;
	
	private String piFirst;
	
	private String piLast;

	private List<TimepointDetail> timepoints = new ArrayList<>();
	
	public String getIrbNumber() {
		return irbNumber;
	}

	public void setIrbNumber(String irbNumber) {
		this.irbNumber = irbNumber;
	}

	public String getFacility() {
		return facility;
	}

	public void setFacility(String facility) {
		this.facility = facility;
	}

	public String getPiAddress() {
		return piAddress;
	}

	public void setPiAddress(String piAddress) {
		this.piAddress = piAddress;
	}

	public List<TimepointDetail> getTimepoints() {
		return timepoints;
	}

	public void setTimepoints(List<TimepointDetail> timepoints) {
		this.timepoints = timepoints;
	}

	public String getPiFirst() {
		return piFirst;
	}

	public void setPiFirst(String piFirst) {
		this.piFirst = piFirst;
	}

	public String getPiLast() {
		return piLast;
	}

	public void setPiLast(String piLast) {
		this.piLast = piLast;
	}

	public boolean hasErrors() {
		return isErroneous() || timepoints.stream().anyMatch(TimepointDetail::hasErrors);
	}

	public boolean hasModifiedTimepoints() {
		return timepoints.stream().anyMatch(t -> t.isUpdated() || t.hasModifiedCollections());
	}
}
