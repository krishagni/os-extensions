
package com.krishagni.openspecimen.asig.events;

import java.util.Date;

public class PatientDetail {

	private String patientId;

	private String hospitalUr;

	private Long clinicId;

	private int status;

	private Boolean consent;
	
	private String siteName;

	private Date dateOfStatusChange;

	private Date lastContactDate;

	public String getPatientId() {
		return patientId;
	}

	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}
	
	public String getHospitalUr() {
		return hospitalUr;
	}

	public void setHospitalUr(String hospitalUr) {
		this.hospitalUr = hospitalUr;
	}

	public Long getClinicId() {
		return clinicId;
	}

	public void setClinicId(Long clinicId) {
		this.clinicId = clinicId;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Boolean getConsent() {
		return consent;
	}

	public void setConsent(Boolean consent) {
		this.consent = consent;
	}

	public Date getDateOfStatusChange() {
		return dateOfStatusChange;
	}

	public void setDateOfStatusChange(Date dateOfStatusChange) {
		this.dateOfStatusChange = dateOfStatusChange;
	}

	public Date getLastContactDate() {
		return lastContactDate;
	}

	public void setLastContactDate(Date lastContactDate) {
		this.lastContactDate = lastContactDate;
	}

	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

}
