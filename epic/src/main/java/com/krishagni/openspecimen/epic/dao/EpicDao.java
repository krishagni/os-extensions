package com.krishagni.openspecimen.epic.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierResponseDetail;
import com.krishagni.openspecimen.epic.events.CprDetail;
import com.krishagni.openspecimen.epic.events.EpicConsentResponse;
import com.krishagni.openspecimen.epic.events.EpicParticipantDetail;
import com.krishagni.openspecimen.epic.events.EpicPmiDetail;

public interface EpicDao {

	public List<EpicParticipantDetail> getEpicParticipantDetails(Connection connection) throws SQLException;

	public Long getOsParticipantId(Connection connection, String sourcePartId, String partSource) throws SQLException;

	public Set<String> getRace(Connection connection, String sourcePartId, String partSource) throws SQLException;

	public void insertMapping(Connection connection, Long origPartId, String sourcePartId, String partSource,
			String mrn) throws SQLException;

	public void updateMapping(Connection connection, Long origPartId, String sourcePartId, String partSource,
			String mrn) throws SQLException;

	public List<EpicPmiDetail> getPmis(Connection connection, String sourcePartId, String partSource, boolean updateFlag)
			throws SQLException;

//	public List<EpicPmiDetail> getPmisWithoutFlag(Connection connection, String sourcePartId, String partSource)
//			throws SQLException;

	public List<CprDetail> getCprDetails(Connection connection, String sourcePartId, String partSource, boolean updateFlag)
			throws SQLException;

//	public List<CprDetail> getCprDetailsWithoutFlag(Connection conn, String sourcePartId, String partSource)
//			throws SQLException;

	public List<ConsentDetail> getConsents(Connection conn, String sourcePartId, String partSource, Long participantId)
			throws SQLException;

	public List<ConsentTierResponseDetail> getConsentDetails(Connection connection, String sourcePartId,
			String partSource, String irbID, String shortTitle) throws SQLException;

	public List<EpicConsentResponse> getConsentDetails(Connection conn, String sourcePartId, String partSource)
			throws SQLException;

	public Long getCpIdByIrbId(String irbId, String shortTitle);

	public Long getCprId(Long participantId, Long cpId);

	public boolean hasSpecimens(Long participantId);

	public void updateAuditLog(Connection connection, String staginPartIdSource, String epicParticipantId,
			String successMsg, String errorMsg, String customMsg) throws SQLException;

	public void updateCatissueStagingAudit(Connection conn, int added_count, int edited_count, int failed_count)
			throws SQLException;

	public Participant getParticipant(Long id);

	public Long getParticipantIdFromMrn(Connection connection, EpicParticipantDetail epicPart) throws SQLException;

}
