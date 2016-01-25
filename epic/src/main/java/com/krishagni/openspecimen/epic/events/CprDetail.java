
package com.krishagni.openspecimen.epic.events;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierResponseDetail;

public class CprDetail {

	private Long cprId;

	private String ppid;

	private Date registrationDate;

	private Long participantId;

	private Long cpId;

	private String activityStatus;

	private String signedConsentDocumentURL;

	private Date consentSignatureDate;

	private String consentWitnessName;

	private List<ConsentTierResponseDetail> consentResponseList = new ArrayList<ConsentTierResponseDetail>();

	private String consentWithdrawalOption;

	private String isConsentAvailable;

	private String barcode;

	private String irbID;

	public String getIrbID() {
		return irbID;
	}

	public void setIrbID(String irbID) {
		this.irbID = irbID;
	}

	public String getConsentWitnessName() {
		return consentWitnessName;
	}

	public void setConsentWitnessName(String consentWitnessName) {
		this.consentWitnessName = consentWitnessName;
	}

	public Long getCprId() {
		return cprId;
	}

	public void setCprId(Long cprId) {
		this.cprId = cprId;
	}

	public String getPpid() {
		return ppid;
	}

	public void setPpid(String ppid) {
		this.ppid = ppid;
	}

	public Date getRegistrationDate() {
		return registrationDate;
	}

	public void setRegistrationDate(Date registrationDate) {
		this.registrationDate = registrationDate;
	}

	public Long getParticipantId() {
		return participantId;
	}

	public void setParticipantId(Long participantId) {
		this.participantId = participantId;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public String getSignedConsentDocumentURL() {
		return signedConsentDocumentURL;
	}

	public void setSignedConsentDocumentURL(String signedConsentDocumentURL) {
		this.signedConsentDocumentURL = signedConsentDocumentURL;
	}

	public Date getConsentSignatureDate() {
		return consentSignatureDate;
	}

	public void setConsentSignatureDate(Date consentSignatureDate) {
		this.consentSignatureDate = consentSignatureDate;
	}

	public List<ConsentTierResponseDetail> getConsentResponseList() {
		return consentResponseList;
	}

	public void setConsentResponseList(List<ConsentTierResponseDetail> consentResponseList) {
		this.consentResponseList = consentResponseList;
	}

	public String getConsentWithdrawalOption() {
		return consentWithdrawalOption;
	}

	public void setConsentWithdrawalOption(String consentWithdrawalOption) {
		this.consentWithdrawalOption = consentWithdrawalOption;
	}

	public String getIsConsentAvailable() {
		return isConsentAvailable;
	}

	public void setIsConsentAvailable(String isConsentAvailable) {
		this.isConsentAvailable = isConsentAvailable;
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

}
