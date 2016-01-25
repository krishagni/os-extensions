package com.krishagni.openspecimen.spr.events;

/**
 * <h1>This class contains the fields of a lab result (including a collection of components).</h1>
 * @author Bob Lange
 * @since 2015-05-30
 *
 */
public class LabResult {
	private String category;
	private String testCode;
	private String testShortDesc;
	private String testDesc;
	private String collectionDateTime;
	private String resultStatus;
	private String orderingProvider;
	private String specimenSource;
	private String specimenId;
	private String bodySite;
	private String placerOrderNo;
	private String performingLab;
	private String specimenComment;
	private LabComponent[] components;
	
	public String getCategory() {
		return category;
	}
	public String getTestCode() {
		return testCode;
	}
	public String getTestShortDesc() {
		return testShortDesc;
	}
	public String getTestDesc() {
		return testDesc;
	}
	public String getCollectionDateTime() {
		return collectionDateTime;
	}
	public String getResultStatus() {
		return resultStatus;
	}
	public String getOrderingProvider() {
		return orderingProvider;
	}
	public String getSpecimenSource() {
		return specimenSource;
	}
	public String getSpecimenId() {
		return specimenId;
	}
	public String getBodySite() {
		return bodySite;
	}
	public String getPlacerOrderNo() {
		return placerOrderNo;
	}
	public String getPerformingLab() {
		return performingLab;
	}
	public String getSpecimenComment() {
		return specimenComment;
	}
	public LabComponent[] getComponents() {
		return components;
	}
	
	

}
