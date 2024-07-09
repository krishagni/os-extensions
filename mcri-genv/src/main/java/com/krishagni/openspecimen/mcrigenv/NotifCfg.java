package com.krishagni.openspecimen.mcrigenv;

import java.util.ArrayList;
import java.util.List;

public class NotifCfg {
	private List<String> cps;

	private String jmsConnectionFactory;

	private String jmsNotifQueue;

	private List<String> emailNotifTypesWhiteList = new ArrayList<>();

	private List<String> emailNotifTypesBlackList = new ArrayList<>();

	private List<String> unacceptableRecvQualities;

	private List<String> recvQualityNotifRcpts;

	private String missingSpecimenFieldName;

	private List<String> missingSpecimenNotifRcpts;

	public List<String> getCps() {
		return cps;
	}

	public void setCps(List<String> cps) {
		this.cps = cps;
	}

	public String getJmsConnectionFactory() {
		return jmsConnectionFactory;
	}

	public void setJmsConnectionFactory(String jmsConnectionFactory) {
		this.jmsConnectionFactory = jmsConnectionFactory;
	}

	public String getJmsNotifQueue() {
		return jmsNotifQueue;
	}

	public void setJmsNotifQueue(String jmsNotifQueue) {
		this.jmsNotifQueue = jmsNotifQueue;
	}

	public List<String> getEmailNotifTypesWhiteList() {
		return emailNotifTypesWhiteList;
	}

	public void setEmailNotifTypesWhiteList(List<String> emailNotifTypesWhiteList) {
		this.emailNotifTypesWhiteList = emailNotifTypesWhiteList;
	}

	public List<String> getEmailNotifTypesBlackList() {
		return emailNotifTypesBlackList;
	}

	public void setEmailNotifTypesBlackList(List<String> emailNotifTypesBlackList) {
		this.emailNotifTypesBlackList = emailNotifTypesBlackList;
	}

	public List<String> getUnacceptableRecvQualities() {
		return unacceptableRecvQualities;
	}

	public void setUnacceptableRecvQualities(List<String> unacceptableRecvQualities) {
		this.unacceptableRecvQualities = unacceptableRecvQualities;
	}

	public List<String> getRecvQualityNotifRcpts() {
		return recvQualityNotifRcpts;
	}

	public void setRecvQualityNotifRcpts(List<String> recvQualityNotifRcpts) {
		this.recvQualityNotifRcpts = recvQualityNotifRcpts;
	}

	public String getMissingSpecimenFieldName() {
		return missingSpecimenFieldName;
	}

	public void setMissingSpecimenFieldName(String missingSpecimenFieldName) {
		this.missingSpecimenFieldName = missingSpecimenFieldName;
	}

	public List<String> getMissingSpecimenNotifRcpts() {
		return missingSpecimenNotifRcpts;
	}

	public void setMissingSpecimenNotifRcpts(List<String> missingSpecimenNotifRcpts) {
		this.missingSpecimenNotifRcpts = missingSpecimenNotifRcpts;
	}
}
