package com.krishagni.openspecimen.wcmc.epic;

public abstract class EpicSourceDbInfo {
	private String source;
	
	private String participantSqlQry;

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getParticipantSqlQry() {
		return participantSqlQry;
	}

	public void setParticipantSqlQry(String participantSqlQry) {
		this.participantSqlQry = participantSqlQry;
	}
}
