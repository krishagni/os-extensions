
package com.krishagni.openspecimen.epic.events;

import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierResponseDetail;

public class EpicConsentResponse {

	private String statement;

	private String response;
	
	private String irbId;
	
	private String cpShortTitle;
	
	private Boolean isUpdatable;
	
	public String getStatement() {
		return statement;
	}

	
	public void setStatement(String statement) {
		this.statement = statement;
	}

	
	public String getResponse() {
		return response;
	}

	
	public void setResponse(String response) {
		this.response = response;
	}

	
	public String getIrbId() {
		return irbId;
	}

	
	public void setIrbId(String irbId) {
		this.irbId = irbId;
	}

	
	public String getCpShortTitle() {
		return cpShortTitle;
	}

	
	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public Boolean isUpdatable() {
		return isUpdatable;
	}

	public void setIsUpdatable(Boolean isUpdatable) {
		this.isUpdatable = isUpdatable;
	}


	public static ConsentTierResponseDetail to(EpicConsentResponse res) {
		ConsentTierResponseDetail response = new ConsentTierResponseDetail();
		response.setResponse(res.getResponse());
		response.setStatement(res.getStatement());
		return response;
	}
	
	

}
