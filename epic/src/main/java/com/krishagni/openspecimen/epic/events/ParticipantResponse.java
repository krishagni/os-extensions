
package com.krishagni.openspecimen.epic.events;

import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.openspecimen.epic.util.ResponseStatus;

public class ParticipantResponse {

	private ParticipantDetail participantDetailsDTO;

	private ParticipantDetail mergeFromParticipantDetailsDTO;

	private ResponseStatus participantResponseStatusEnum;

	private String message;

	public ParticipantDetail getMergeFromParticipantDetailsDTO() {
		return mergeFromParticipantDetailsDTO;
	}

	public void setMergeFromParticipantDetailsDTO(ParticipantDetail mergeFromParticipantDetailsDTO) {
		this.mergeFromParticipantDetailsDTO = mergeFromParticipantDetailsDTO;
	}

	public ParticipantDetail getParticipantDetail() {
		return participantDetailsDTO;
	}

	public void setParticipantDetail(ParticipantDetail participantDetailsDTO) {
		this.participantDetailsDTO = participantDetailsDTO;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public ResponseStatus getStatus() {
		return participantResponseStatusEnum;
	}

	public void setResponseStatus(ResponseStatus participantResponseStatusEnum) {
		this.participantResponseStatusEnum = participantResponseStatusEnum;
	}

}
