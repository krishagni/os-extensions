package com.krishagni.openspecimen.demo.events;

import com.krishagni.catissueplus.core.administrative.events.UserDetail;

public class ClientDetail extends UserDetail {
	private String demoReq;
	
	private String legacyData;
	
	private Boolean plannedCollection;
	
	private Boolean unplannedCollection;
	
	private String specimenCollection;
	
	private String specimenDistribution;
	
	private String specimenDistributionOthers;
	
	private Boolean cdms;
	
	private Boolean barcodePrinters;
	
	private Boolean cerner;
	
	private Boolean redcap;
	
	private Boolean openClinica;
	
	private Boolean velos;
	
	private Boolean osIntegration;
	
	private String osIntegrationOthers;
	
	private String osIntegrationValue;
	
	private String serverNeeds;
	
	private String explanation;

	public String getDemoReq() {
		return demoReq;
	}

	public void setDemoReq(String demoReq) {
		this.demoReq = demoReq;
	}

	public String getLegacyData() {
		return legacyData;
	}

	public void setLegacyData(String legacyData) {
		this.legacyData = legacyData;
	}

	public Boolean getPlannedCollection() {
		return plannedCollection;
	}

	public void setPlannedCollection(Boolean plannedCollection) {
		this.plannedCollection = plannedCollection;
	}

	public Boolean getUnplannedCollection() {
		return unplannedCollection;
	}

	public void setUnplannedCollection(Boolean unplannedCollection) {
		this.unplannedCollection = unplannedCollection;
	}

	public String getSpecimenCollection() {
		return specimenCollection;
	}

	public void setSpecimenCollection(String specimenCollection) {
		this.specimenCollection = specimenCollection;
	}

	public String getSpecimenDistribution() {
		return specimenDistribution;
	}

	public void setSpecimenDistribution(String specimenDistribution) {
		this.specimenDistribution = specimenDistribution;
	}

	public String getSpecimenDistributionOthers() {
		return specimenDistributionOthers;
	}

	public void setSpecimenDistributionOthers(String specimenDistributionOthers) {
		this.specimenDistributionOthers = specimenDistributionOthers;
	}

	public Boolean getCdms() {
		return cdms;
	}

	public void setCdms(Boolean cdms) {
		this.cdms = cdms;
	}

	public Boolean getBarcodePrinters() {
		return barcodePrinters;
	}

	public void setBarcodePrinters(Boolean barcodePrinters) {
		this.barcodePrinters = barcodePrinters;
	}

	public Boolean getCerner() {
		return cerner;
	}

	public void setCerner(Boolean cerner) {
		this.cerner = cerner;
	}

	public Boolean getRedcap() {
		return redcap;
	}

	public void setRedcap(Boolean redcap) {
		this.redcap = redcap;
	}

	public Boolean getOpenClinica() {
		return openClinica;
	}

	public void setOpenClinica(Boolean openClinica) {
		this.openClinica = openClinica;
	}

	public Boolean getVelos() {
		return velos;
	}

	public void setVelos(Boolean velos) {
		this.velos = velos;
	}

	public Boolean getOsIntegration() {
		return osIntegration;
	}

	public void setOsIntegration(Boolean osIntegration) {
		this.osIntegration = osIntegration;
	}

	public String getOsIntegrationOthers() {
		return osIntegrationOthers;
	}

	public void setOsIntegrationOthers(String osIntegrationOthers) {
		this.osIntegrationOthers = osIntegrationOthers;
	}

	public String getOsIntegrationValue() {
		return osIntegrationValue;
	}

	public void setOsIntegrationValue(String osIntegrationValue) {
		this.osIntegrationValue = osIntegrationValue;
	}

	public String getServerNeeds() {
		return serverNeeds;
	}

	public void setServerNeeds(String serverNeeds) {
		this.serverNeeds = serverNeeds;
	}

	public String getExplanation() {
		return explanation;
	}

	public void setExplanation(String explanation) {
		this.explanation = explanation;
	}
}