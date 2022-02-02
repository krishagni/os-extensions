package com.krishagni.openspecimen.msk2.events;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CarsBiospecimenDetail extends ImportLogDetail {

	private String patientId;

	private String irbNumber;

	private String patientStudyId;
	
	private String facility;
	
	private String firstName;
	
	private String lastName;

	private String middleName;

	private Date dob;
	
	private String mrn;

	private List<TimepointDetail> timepoints = new ArrayList<>();

	private Date lastUpdated;

	public String getPatientId() {
		return patientId;
	}

	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}

	public String getIrbNumber() {
		return irbNumber;
	}

	public void setIrbNumber(String irbNumber) {
		this.irbNumber = irbNumber;
	}

	public String getPatientStudyId() {
		return patientStudyId;
	}

	public void setPatientStudyId(String patientStudyId) {
		this.patientStudyId = patientStudyId;
	}

	public String getFacility() {
		return facility;
	}

	public void setFacility(String facility) {
		this.facility = facility;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public Date getDob() {
		return dob;
	}

	public void setDob(Date dob) {
		this.dob = dob;
	}

	public String getMrn() {
		return mrn;
	}

	public void setMrn(String mrn) {
		this.mrn = mrn;
	}

	public List<TimepointDetail> getTimepoints() {
		return timepoints;
	}

	public void setTimepoints(List<TimepointDetail> timepoints) {
		this.timepoints = timepoints;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}
