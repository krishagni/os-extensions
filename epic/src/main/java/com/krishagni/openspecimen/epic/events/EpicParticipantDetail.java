
package com.krishagni.openspecimen.epic.events;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EpicParticipantDetail {

	private String id;  

	private String source;

	private Long osId; 

	private String changeType;

	private String oldId;

	private String firstName;

	private String middleName;

	private String lastName;

	private Date birthDate;

	private String gender;

	private String vitalStatus;

	private Date deathDate;

	private String ethnicity;
	
	private Boolean isUpdatable;
	
	private Boolean isCprUpdatable;
	
	private Boolean isMrnUpdatable;
	
	private Boolean isConsentsUpdatable;

	private List<EpicPmiDetail> pmiDetails = new ArrayList<EpicPmiDetail>();

	private List<String> raceList = new ArrayList<String>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public Long getOsId() {
		return osId;
	}

	public void setOsId(Long osId) {
		this.osId = osId;
	}

	public String getChangeType() {
		return changeType;
	}

	public void setChangeType(String changeType) {
		this.changeType = changeType;
	}

	public String getOldId() {
		return oldId;
	}

	public void setOldId(String oldId) {
		this.oldId = oldId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getVitalStatus() {
		return vitalStatus;
	}

	public void setVitalStatus(String vitalStatus) {
		this.vitalStatus = vitalStatus;
	}

	public Date getDeathDate() {
		return deathDate;
	}

	public void setDeathDate(Date deathDate) {
		this.deathDate = deathDate;
	}

	public String getEthnicity() {
		return ethnicity;
	}

	public void setEthnicity(String ethnicity) {
		this.ethnicity = ethnicity;
	}

	public List<EpicPmiDetail> getPmiDetails() {
		return pmiDetails;
	}

	public void setPmiDetails(List<EpicPmiDetail> pmiDetails) {
		this.pmiDetails = pmiDetails;
	}

	public List<String> getRaceList() {
		return raceList;
	}

	public void setRaceList(List<String> raceList) {
		this.raceList = raceList;
	}

	public Boolean isUpdatable() {
		return isUpdatable;
	}
	
	public void setIsUpdatable(Boolean isUpdatable) {
		this.isUpdatable = isUpdatable;
	}

	public Boolean isCprUpdatable() {
		return isCprUpdatable;
	}
	
	public void setIsCprUpdatable(Boolean isCprUpdatable) {
		this.isCprUpdatable = isCprUpdatable;
	}
	
	public Boolean isMrnUpdatable() {
		return isMrnUpdatable;
	}
	
	public void setIsMrnUpdatable(Boolean isMrnUpdatable) {
		this.isMrnUpdatable = isMrnUpdatable;
	}

	public Boolean isConsentsUpdatable() {
		return isConsentsUpdatable;
	}

	public void setIsConsentsUpdatable(Boolean isConsentsUpdatable) {
		this.isConsentsUpdatable = isConsentsUpdatable;
	}

}
