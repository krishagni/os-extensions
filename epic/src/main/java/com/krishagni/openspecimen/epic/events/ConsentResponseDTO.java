
package com.krishagni.openspecimen.epic.events;

public class ConsentResponseDTO {

	private String consentStatment;

	private String participantResponses;

	public String getParticipantResponses() {
		return participantResponses;
	}

	public void setParticipantResponses(String participantResponses) {
		this.participantResponses = participantResponses;
	}

	public String getConsentStatment() {
		return consentStatment;
	}

	public void setConsentStatment(String consentStatment) {
		this.consentStatment = consentStatment;
	}

}
