
package com.krishagni.openspecimen.epic.events;

import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;

public class EpicMergeFailedDTO {

	private String oldPartSourceID;

	private String partSourceID;

	private ParticipantDetail oldParticipantDetailsDTO;

	private ParticipantDetail participantDetailsDTO;

	private String partSource;

	private String changeType;

	public String getOldPartSourceID() {
		return oldPartSourceID;
	}

	public void setOldPartSourceID(String oldPartSourceID) {
		this.oldPartSourceID = oldPartSourceID;
	}

	public String getPartSourceID() {
		return partSourceID;
	}

	public void setPartSourceID(String partSourceID) {
		this.partSourceID = partSourceID;
	}

	public ParticipantDetail getParticipantDetailsDTO() {
		return participantDetailsDTO;
	}

	public void setParticipantDetailsDTO(ParticipantDetail participantDetailsDTO) {
		this.participantDetailsDTO = participantDetailsDTO;
	}

	public String getChangeType() {
		return changeType;
	}

	public void setChangeType(String changeType) {
		this.changeType = changeType;
	}

	public ParticipantDetail getOldParticipantDetailsDTO() {
		return oldParticipantDetailsDTO;
	}

	public void setOldParticipantDetailsDTO(ParticipantDetail oldParticipantDetailsDTO) {
		this.oldParticipantDetailsDTO = oldParticipantDetailsDTO;
	}

	public String getPartSource() {
		return partSource;
	}

	public void setPartSource(String partSource) {
		this.partSource = partSource;
	}

}
