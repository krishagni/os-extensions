package com.openspecimen.ext.participant.source;

import java.sql.SQLException;
import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.events.StagedParticipantDetail;

public interface ExternalParticipantSource {
	List<StagedParticipantDetail> getParticipants();

	String getName();
	
	Boolean hasRows();
	
	void closeConnection() throws SQLException;
}
