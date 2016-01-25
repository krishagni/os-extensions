package com.krishagni.openspecimen.spr.events;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;

public class LabHeader {
	private String epicMrn;
	private String facilityCode;
	private String key;
	private String collectionDateTime;
	private String category;
	private String testCode;
	private String testShortDesc;
	private String testDesc;
	private String resultStatus;
	
	/**
	 * Returns the MRN of patient whose lab test this is.
	 * @return String
	 */
	public String getEpicMrn() {
		return epicMrn;
	}

	/**
	 * Returns the facility code for this lab test.
	 * @return String
	 */
	public String getFacilityCode() {
		return facilityCode;
	}
	
	/**
	 * Returns the key used to look up this lab test via the REST webservice.
	 * @return String
	 */
	public String getKey() {
		return key;
	}
	
	/**
	 * Returns the Base64 encoded key used to look up this lab test via the REST webservice.
	 * @return String
	 */
	public String getBase64Key() {
		try {
			return new String(Base64.encodeBase64(key.getBytes()),"UTF-8");
		} catch(UnsupportedEncodingException uee) {
			return "";
		}
	}
	
	/**
	 * Returns the Pathology ID (Accession #)
	 * @return String
	 */
	public String getPathId() {
		int indx = key.indexOf("^");
		return key.substring(key.indexOf(':') + 1, indx == -1 ? key.length() -1 : indx);
	}

	/**
	 * Returns the Collection date and time
	 * @return String
	 */
	public String getCollectionDateTime() {
		return collectionDateTime;
	}
	
	/**
	 * Returns the lab test category (e.g., Anatomic Pathology, Chemistry)
	 * @return String
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * Returns the test code
	 * @return String
	 */
	public String getTestCode() {
		return testCode;
	}

	/**
	 * Returns the short name of the test
	 * @return String
	 */
	public String getTestShortDesc() {
		return testShortDesc;
	}

	/**
	 * Returns the long name of the test
	 * @return String
	 */
	public String getTestDesc() {
		return testDesc;
	}

	/**
	 * Returns the test result status
	 * @return String
	 */
	public String getResultStatus() {
		return resultStatus;
	}
}
