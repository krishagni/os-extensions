package com.krishagni.os.jhuepic.events;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class EpicPatient {

	@JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	private Date DateOfBirth;
	
	private String Sex;
	
	private String[] Race;
	
	private String EthnicGroup;
	
	private List<PmiDetail> IDs;
	
	private NameComponents NameComponents;
	
	public Date getDateOfBirth() {
		return DateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.DateOfBirth = dateOfBirth;
	}

	public String getSex() {
		return Sex;
	}

	public void setSex(String sex) {
		this.Sex = sex;
	}

	public String[] getRace() {
		return Race;
	}

	public void setRace(String[] race) {
		this.Race = race;
	}

	public String getEthnicGroup() {
		return EthnicGroup;
	}

	public void setEthnicGroup(String ethnicGroup) {
		this.EthnicGroup = ethnicGroup;
	}

	public NameComponents getNameComponents() {
		return NameComponents;
	}

	public void setNameComponents(NameComponents nameComponents) {
		this.NameComponents = nameComponents;
	}
	
	public List<PmiDetail> getIDs() {
		return IDs;
	}

	public void setIDs(List<PmiDetail> iDs) {
		IDs = iDs;
	}

	class NameComponents {
		String LastName;
		String FirstName;
		public String getLastName() {
			return LastName;
		}
		public void setLastName(String lastName) {
			this.LastName = lastName;
		}
		public String getFirstName() {
			return FirstName;
		}
		public void setFirstName(String firstName) {
			this.FirstName = firstName;
		}
	}
}
