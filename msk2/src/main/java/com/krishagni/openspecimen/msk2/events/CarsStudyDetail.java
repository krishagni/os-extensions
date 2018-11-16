package com.krishagni.openspecimen.msk2.events;

import java.util.ArrayList;
import java.util.List;

public class CarsStudyDetail extends ImportLogDetail {
	private String irbNumber;

	private String facility;

	private String piAddress;

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
}
