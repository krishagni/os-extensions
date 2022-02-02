package com.krishagni.os.jhuepic;

import java.util.Date;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EpicPatient {
	@JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	private Date DateOfBirth;
	
	private String Sex;
	
	private String[] Race;
	
	private String EthnicGroup;

	private String Status;

	@JsonProperty("IDs")
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

	public String getStatus() {
		return Status;
	}

	public void setStatus(String status) {
		Status = status;
	}

	public List<PmiDetail> getIds() {
		return IDs;
	}

	public void setIds(List<PmiDetail> iDs) {
		IDs = iDs;
	}

	public NameComponents getNameComponents() {
		return NameComponents;
	}

	public void setNameComponents(NameComponents nameComponents) {
		this.NameComponents = nameComponents;
	}

	public String getFirstName() {
		return NameComponents != null ? NameComponents.getFirstName() : null;
	}

	public String getLastName() {
		return NameComponents != null ? NameComponents.getLastName() : null;
	}

	public String getMiddleName() {
		return NameComponents != null ? NameComponents.getMiddleName() : null;
	}

	public static class NameComponents {
		private String LastName;

		private String FirstName;

		private String MiddleName;

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

		public String getMiddleName() {
			return MiddleName;
		}

		public void setMiddleName(String middleName) {
			MiddleName = middleName;
		}

	}

	public static class PmiDetail {
		@JsonProperty("ID")
		private String ID;

		@JsonProperty("Type")
		private String Type;

		public String getId() {
			return ID;
		}

		public void setId(String iD) {
			ID = iD;
		}

		public String getType() {
			return Type;
		}

		public void setType(String type) {
			Type = type;
		}
	}

	public String toString() {
		return getNameComponents().getFirstName() + "-" + getNameComponents().getLastName();
	}
}
