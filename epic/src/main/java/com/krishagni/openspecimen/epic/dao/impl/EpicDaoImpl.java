
package com.krishagni.openspecimen.epic.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierResponseDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.DaoFactoryImpl;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.openspecimen.epic.dao.EpicDao;
import com.krishagni.openspecimen.epic.events.CprDetail;
import com.krishagni.openspecimen.epic.events.EpicConsentResponse;
import com.krishagni.openspecimen.epic.events.EpicParticipantDetail;
import com.krishagni.openspecimen.epic.events.EpicPmiDetail;

public class EpicDaoImpl implements EpicDao {
	private static final Log logger = LogFactory.getLog(EpicDaoImpl.class);

	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public List<EpicParticipantDetail> getEpicParticipantDetails(Connection conn) throws SQLException {
		PreparedStatement statement = null;
		ResultSet rs = null;
		List<EpicParticipantDetail> result = new ArrayList<EpicParticipantDetail>();
		try {
			statement = conn.prepareStatement(getEpicParticipantDetail);
			rs = statement.executeQuery();
			while (rs.next()) {
				result.add(populateEpicObj(rs, conn));
			}
		} catch (SQLException e) {
			logger.error("SQL error: No records processed.......", e);
		} finally {
			closeStatement(statement);
			closeResultSet(rs);
		}
		return result;
	}

	@Override
	public Long getOsParticipantId(Connection conn, String sourcePartId, String partSource) throws SQLException {
		ResultSet rs = null;
		PreparedStatement statement = null;
		Long osId = null;

		try {
			statement = conn.prepareStatement(getOsParticipantId);
			statement.setString(1, sourcePartId);
			statement.setString(2, partSource);
			rs = statement.executeQuery();
			if (rs.next()) {
				osId = rs.getLong("CATISSUE_PART_ID");
			}
		} finally {
			closeStatement(statement);
			closeResultSet(rs);
		}
		return osId;
	}

	@Override
	public void insertMapping(Connection conn, Long origPartId, String sourcePartId, String partSource, String mrn)
			throws SQLException {
		PreparedStatement statement = null;
		try {
			statement = conn.prepareStatement(instertMapping);
			statement.setLong(1, origPartId);
			statement.setString(2, sourcePartId);
			statement.setString(3, partSource);
			statement.setString(4, mrn);
			statement.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
			statement.executeUpdate();
		} finally {
			closeStatement(statement);
		}
	}

	@Override
	public void updateMapping(Connection conn, Long origPartId, String sourcePartId, String partSource, String mrn)
			throws SQLException {
		PreparedStatement statement = null;
		try {
			statement = conn.prepareStatement(updatingMapping);
			statement.setString(1, sourcePartId);
			statement.setString(2, partSource);
			statement.executeUpdate();
			insertMapping(conn, origPartId, sourcePartId, partSource, mrn);
		} finally {
			closeStatement(statement);
		}
	}

	@Override
	public Set<String> getRace(Connection conn, String sourcePartId, String partSource) throws SQLException {
		PreparedStatement statement = null;
		ResultSet rs = null;
		Set<String> race = new HashSet<String>();
		try {
			statement = conn.prepareStatement(getRace);
			statement.setString(1, sourcePartId);
			statement.setString(2, partSource);
			rs = statement.executeQuery();
			while (rs.next()) {
				race.add(getString(rs.getObject(1)));
			}
		} finally {
			closeStatement(statement);
			closeResultSet(rs);
		}
		return race;
	}

	@Override
	public List<EpicPmiDetail> getPmis(Connection conn, String sourcePartId, String partSource, boolean updateFlag) throws SQLException {
		PreparedStatement statement = null;
		ResultSet rs = null;
		List<EpicPmiDetail> pmis = new ArrayList<EpicPmiDetail>();
		try {
			if (updateFlag) {
				statement = conn.prepareStatement(getMrnDetails);
			} else {
				statement = conn.prepareStatement(getMrnDetailsWithoutFlag);
			}
			statement.setString(1, sourcePartId);
			statement.setString(2, partSource);
			rs = statement.executeQuery();
			while (rs.next()) {
				EpicPmiDetail pmi = new EpicPmiDetail();
				pmi.setSiteName(getString(rs.getObject(1)));
				pmi.setMrnValue(getString(rs.getObject(2)));
				pmi.setNewMrnValue(getString(rs.getObject(3)));
				pmis.add(pmi);
			}
		} finally {
			closeStatement(statement);
			closeResultSet(rs);
		}

		return pmis;
	}

//	@Override
//	public List<EpicPmiDetail> getPmisWithoutFlag(Connection conn, String sourcePartId, String partSource)
//			throws SQLException {
//		PreparedStatement statement = null;
//		ResultSet rs = null;
//		List<EpicPmiDetail> pmis = new ArrayList<EpicPmiDetail>();
//		try {
//			statement = conn.prepareStatement(getMrnDetailsWithoutFlag);
//			statement.setString(1, sourcePartId);
//			statement.setString(2, partSource);
//			rs = statement.executeQuery();
//			while (rs.next()) {
//				EpicPmiDetail pmi = new EpicPmiDetail();
//				pmi.setSiteName(getString(rs.getObject(1)));
//				pmi.setMrnValue(getString(rs.getObject(2)));
//				pmi.setNewMrnValue(getString(rs.getObject(3)));
//				pmis.add(pmi);
//			}
//		} finally {
//			closeStatement(statement);
//			closeResultSet(rs);
//		}
//
//		return pmis;
//	}

	@Override
	public List<CprDetail> getCprDetails(Connection conn, String sourcePartId, String partSource, boolean updateFlag) throws SQLException {
		PreparedStatement statement = null;
		ResultSet rs = null;
		List<CprDetail> cprDetails = new ArrayList<CprDetail>();
		try {
			if (updateFlag) {
				statement = conn.prepareStatement(getCprDetail);
			} else {
				statement = conn.prepareStatement(getCprDetailWithoutFlag);
			}
			statement.setString(1, sourcePartId);
			statement.setString(2, partSource);
			rs = statement.executeQuery();
			while (rs.next()) {
				CprDetail detail = new CprDetail();
				detail.setRegistrationDate(getDate(rs.getObject(1)));
				detail.setConsentSignatureDate(getDate(rs.getObject(2)));
				detail.setIrbID(getString(rs.getObject(3)));
				detail.setShortTitle(getString(rs.getObject(4)));
				detail.setConsentResponseList(
						getConsentDetails(conn, sourcePartId, partSource, detail.getIrbID(), detail.getShortTitle()));
				cprDetails.add(detail);
			}
		} finally {
			closeStatement(statement);
			closeResultSet(rs);
		}
		return cprDetails;
	}

//	@Override
//	public List<CprDetail> getCprDetailsWithoutFlag(Connection conn, String sourcePartId, String partSource)
//			throws SQLException {
//		PreparedStatement statement = null;
//		ResultSet rs = null;
//		List<CprDetail> cprDetails = new ArrayList<CprDetail>();
//		try {
//			statement = conn.prepareStatement(getCprDetailWithoutFlag);
//			statement.setString(1, sourcePartId);
//			statement.setString(2, partSource);
//			rs = statement.executeQuery();
//			while (rs.next()) {
//				CprDetail detail = new CprDetail();
//				detail.setRegistrationDate(getDate(rs.getObject(1)));
//				detail.setConsentSignatureDate(getDate(rs.getObject(2)));
//				detail.setIrbID(getString(rs.getObject(3)));
//				detail.setShortTitle(getString(rs.getObject(4)));
//				detail.setConsentResponseList(
//						getConsentDetails(conn, sourcePartId, partSource, detail.getIrbID(), detail.getShortTitle()));
//				cprDetails.add(detail);
//			}
//		} finally {
//			closeStatement(statement);
//			closeResultSet(rs);
//		}
//		return cprDetails;
//	}

	@Override
	public List<ConsentDetail> getConsents(Connection conn, String sourcePartId, String partSource, Long participantId)
			throws SQLException {
		List<ConsentDetail> list = new ArrayList<ConsentDetail>();
		ResultSet rs = null;
		PreparedStatement statement = null;
		try {
			statement = conn.prepareStatement(getConsentBasicDetails);
			statement.setString(1, sourcePartId);
			statement.setString(2, partSource);
			rs = statement.executeQuery();
			while (rs.next()) {
				String irbId = getString(rs.getObject(1));
				String shortTitle = getString(rs.getObject(2));

				ConsentDetail consentDetail = new ConsentDetail();
				consentDetail.setCprId(getCprId(participantId, getCpIdByIrbId(irbId, shortTitle)));
				consentDetail
						.setConsentTierResponses(getConsentDetails(conn, sourcePartId, partSource, irbId, shortTitle));
				list.add(consentDetail);
			}
		} finally {
			closeStatement(statement);
			closeResultSet(rs);
		}
		return list;
	}

	@Override
	public List<ConsentTierResponseDetail> getConsentDetails(Connection conn, String sourcePartId, String partSource,
			String irbID, String shortTitle) throws SQLException {
		ResultSet rs = null;
		PreparedStatement statement = null;
		List<ConsentTierResponseDetail> consents = new ArrayList<ConsentTierResponseDetail>();

		try {
			statement = conn.prepareStatement(getConsentDetails);
			statement.setString(1, sourcePartId);
			statement.setString(2, partSource);
			rs = statement.executeQuery();
			while (rs.next()) {
				if (irbID.equalsIgnoreCase(getString(rs.getObject(3)))
						&& shortTitle.equalsIgnoreCase(getString(rs.getObject(4)))) {
					ConsentTierResponseDetail response = new ConsentTierResponseDetail();
					response.setStatement(getString(rs.getObject(1)));
					response.setResponse(getString(rs.getObject(2)));
					consents.add(response);
				}
			}
		} finally {
			closeStatement(statement);
			closeResultSet(rs);
		}
		return consents;
	}

	@Override
	public List<EpicConsentResponse> getConsentDetails(Connection conn, String sourcePartId, String partSource)
			throws SQLException {
		ResultSet rs = null;
		PreparedStatement statement = null;
		List<EpicConsentResponse> consents = new ArrayList<EpicConsentResponse>();

		try {
			statement = conn.prepareStatement(getConsentDetails);
			statement.setString(1, sourcePartId);
			statement.setString(2, partSource);
			rs = statement.executeQuery();
			while (rs.next()) {
				EpicConsentResponse response = new EpicConsentResponse();
				response.setStatement(getString(rs.getObject(1)));
				response.setResponse(getString(rs.getObject(2)));

				response.setIrbId(getString(rs.getObject(3)));
				response.setCpShortTitle(getString(rs.getObject(4)));

				// MySQL
				// response.setIsUpdatable(Boolean.valueOf(getString(rs.getObject(5))));

				// Oracle
				response.setIsUpdatable(getString(rs.getObject(5)).equals("1") ? true : false);

				consents.add(response);
			}
		} finally {
			closeStatement(statement);
			closeResultSet(rs);
		}
		return consents;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Long getCpIdByIrbId(String irbId, String shortTitle) {
		List<Object> result = getCurrentSession().createSQLQuery(getCpIdByIrbId).setParameter(0, irbId)
				.setParameter(1, shortTitle).list();

		return result.isEmpty() ? null : Utility.numberToLong(result.get(0));
	}

	@Override
	public Participant getParticipant(Long id) {
		return daoFactory.getParticipantDao().getById(id);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean hasSpecimens(Long participantId) {
		List<Object[]> result = getCurrentSession().createSQLQuery(getCollectedSpecimenCountByPartId)
				.setParameter(0, participantId).list();

		return CollectionUtils.isNotEmpty(result) && String.valueOf(result.get(0)).equals("0") ? false : true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Long getCprId(Long participantId, Long cpId) {
		System.out.println("ParticipantId: " + participantId + " and CpId: " + cpId);
		List<CollectionProtocolRegistration> cprs = getCurrentSession()
				.createQuery("from com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration "
						+ " cpr where  cpr.participant.id = ? and cpr.collectionProtocol.id = ?")
				.setParameter(0, participantId).setParameter(1, cpId).list();
		return cprs.isEmpty() ? null : cprs.iterator().next().getId();
	}

	@Override
	public void updateAuditLog(Connection conn, String staginPartIdSource, String epicParticipantId, String successMsg,
			String errorMsg, String customMsg) throws SQLException {
		PreparedStatement statement = null;
		try {
			statement = conn.prepareStatement(updateParticipantAuditTable);
			statement.setString(1, epicParticipantId);
			statement.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
			statement.setString(3, successMsg);
			statement.setString(4, errorMsg);
			statement.setString(5, staginPartIdSource);
			statement.setString(6, customMsg);
			statement.executeUpdate();
		} finally {
			closeStatement(statement);
		}
	}

	@Override
	public void updateCatissueStagingAudit(Connection conn, int added_count, int edited_count, int failed_count)
			throws SQLException {
		PreparedStatement statement = null;
		try {
			statement = conn.prepareStatement(updateCatissueJobAudit);
			statement.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
			statement.setInt(2, added_count);
			statement.setInt(3, edited_count);
			statement.setInt(4, failed_count);
			statement.executeUpdate();
		} finally {
			if (statement != null) {
				statement.close();
			}
		}

	}

	@Override
	@SuppressWarnings("unchecked")
	public Long getParticipantIdFromMrn(Connection connection, EpicParticipantDetail epicPart) throws SQLException {
		logger.debug("Inside getting Id by MRN");
		List<EpicPmiDetail> pmis = getPmis(connection, epicPart.getId(), epicPart.getSource(), false);
		System.out.println("PMI size : " + pmis == null ? 0 : pmis.size());
		if (CollectionUtils.isEmpty(pmis)) {
			return null;
		}
		EpicPmiDetail pmi = pmis.iterator().next();
		logger.debug("MRN: " + pmi.getMrnValue());
		logger.debug("siteName: " + pmi.getSiteName());
		List<Object> result = getCurrentSession().createSQLQuery(getPartIdByMRN).setParameter(0, pmi.getMrnValue())
				.setParameter(1, pmi.getSiteName()).list();
		return result.isEmpty() ? null : Utility.numberToLong(result.get(0));
	}

	private Session getCurrentSession() {
		return ((DaoFactoryImpl) daoFactory).getSessionFactory().getCurrentSession();
	}

	private EpicParticipantDetail populateEpicObj(ResultSet rs, Connection conn) throws SQLException {
		EpicParticipantDetail epicParticipantDetail = new EpicParticipantDetail();

		epicParticipantDetail.setId(getString(rs.getObject(1)));
		epicParticipantDetail.setSource(getString(rs.getObject(2)));
		epicParticipantDetail.setOsId(Utility.numberToLong(rs.getObject(3)));
		epicParticipantDetail.setChangeType(getString(rs.getObject(4)));
		epicParticipantDetail.setOldId(getString(rs.getObject(5)));
		epicParticipantDetail.setFirstName(getString(rs.getObject(6)));
		epicParticipantDetail.setMiddleName(getString(rs.getObject(7)));
		epicParticipantDetail.setLastName(getString(rs.getObject(8)));
		epicParticipantDetail.setBirthDate(getDate(rs.getObject(9)));
		epicParticipantDetail.setGender(getString(rs.getObject(10)));
		epicParticipantDetail.setVitalStatus(getString(rs.getObject(11)));
		epicParticipantDetail.setDeathDate(getDate(rs.getObject(12)));
		epicParticipantDetail.setEthnicity(getString(rs.getObject(13)));

		// ForOracle

		epicParticipantDetail.setIsUpdatable(getString(rs.getObject(16)).equals("1") ? true : false);

		// For MySQL

		// epicParticipantDetail.setIsUpdatable(Boolean.valueOf(getString(rs.getObject(16))));
		// epicParticipantDetail.setIsCprUpdatable(Boolean.valueOf(getString(rs.getObject(17))));
		// epicParticipantDetail.setIsMrnUpdatable(Boolean.valueOf(getString(rs.getObject(18))));
		// epicParticipantDetail.setIsConsentsUpdatable(Boolean.valueOf(getString(rs.getObject(19))));

		// epicParticipantDetail.setPmiDetails(getPmis(conn,
		// epicParticipantDetail.getId(), epicParticipantDetail.getSource()));

		return epicParticipantDetail;
	}

	private Date getDate(Object object) {
		if (object == null) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(ConfigUtil.getInstance().getDateFmt());
		Date date = null;

		try {
			date = sdf.parse(sdf.format(object));
		} catch (ParseException e) {
			logger.error("Error during date parsing....", e);
		}
		return date;
	}

	private String getString(Object obj) {
		return obj != null ? obj.toString() : "";
	}

	private void closeResultSet(ResultSet rs) throws SQLException {
		if (rs != null) {
			rs.close();
		}
	}

	private void closeStatement(PreparedStatement statement) throws SQLException {
		if (statement != null) {
			statement.close();
		}
	}

	private final String getEpicParticipantDetail = "select "
			+ "  participant.PART_SOURCE_ID,participant.PART_SOURCE,mappinginfo.CATISSUE_PART_ID,history.CHANGE_TYPE, "
			+ "  history.PART_SOURCE_ID OLD_PART_SOURCE_ID,participant.FIRST_NAME,participant.MIDDLE_NAME,participant.LAST_NAME, "
			+ "  participant.DATE_OF_BIRTH,participant.GENDER,participant.VITAL_STATUS,participant.DEATH_DATE, "
			+ "  participant.ETHNICITY, participant.STATUS,participant.PAT_UPDATE_DATE,participant.UPDATE_FLAG "
			+ "from " + "  STAGING_CATISSUE_PATIENT participant "
			+ "  left join STAGING_PART_ID_HISTORY history on history.NEW_PART_SOURCE_ID = participant.PART_SOURCE_ID "
			+ "  and history.PART_SOURCE = participant.PART_SOURCE "
			+ "  left join STAGING_PART_INFO_MAPPING mappinginfo on "
			+ "  (mappinginfo.PART_SOURCE_ID = participant.PART_SOURCE_ID and mappinginfo.PART_SOURCE = participant.PART_SOURCE) "
			+ "  left join STAGING_CATISSUE_PAT_ENROLL cpr on cpr.PART_SOURCE_ID = participant.PART_SOURCE_ID "
			+ "  left join STAGING_CATISSUE_CONSENTS consent on consent.PART_SOURCE_ID = participant.PART_SOURCE_ID "
			+ "  left join STAGING_CATISSUE_MRN mrn on mrn.PART_SOURCE_ID = participant.PART_SOURCE_ID " + "where "
			+ "  (participant.UPDATE_FLAG = 1 or consent.UPDATE_FLAG = 1 or cpr.UPDATE_FLAG = 1 or mrn.UPDATE_FLAG = 1) "
			+ "  group by participant.PART_SOURCE_ID,participant.FIRST_NAME, "
			+ "  participant.MIDDLE_NAME,participant.LAST_NAME,participant.DATE_OF_BIRTH,participant.GENDER, "
			+ "  participant.VITAL_STATUS,participant.DEATH_DATE,participant.STATUS,participant.PAT_UPDATE_DATE, "
			+ "  participant.UPDATE_FLAG,history.PART_SOURCE_ID,mappinginfo.CATISSUE_PART_ID,participant.PART_SOURCE, "
			+ "  history.CHANGE_TYPE,participant.ETHNICITY, participant.UPDATE_FLAG "
			+ "   order by participant.PART_SOURCE_ID";

	private final String getOsParticipantId = "select CATISSUE_PART_ID from STAGING_PART_INFO_MAPPING where PART_SOURCE_ID = ? and PART_SOURCE = ?";

	private final String instertMapping = "insert into STAGING_PART_INFO_MAPPING ( CATISSUE_PART_ID , PART_SOURCE_ID , PART_SOURCE, MRN_VALUE, MAPPING_DATE) values (?,?,?,?,?)";

	private final String updatingMapping = "delete from STAGING_PART_INFO_MAPPING where PART_SOURCE_ID = ? and PART_SOURCE = ? ";

	private final String getRace = "select RACE_VALUE from STAGING_CATISSUE_RACE where PART_SOURCE_ID = ? and PART_SOURCE = ? ";

	private final String getCprDetail = "select "
			+ "  enroll.REGISTRATION_DATE, enroll.CONSENT_DATE, enroll.IRB_ID, enroll.SHORT_TITLE " + "from "
			+ "  STAGING_CATISSUE_PAT_ENROLL enroll " + "where "
			+ "  enroll.PART_SOURCE_ID = ? and enroll.PART_SOURCE = ? and enroll.UPDATE_FLAG = 1";

	private final String getCprDetailWithoutFlag = "select "
			+ "  enroll.REGISTRATION_DATE, enroll.CONSENT_DATE, enroll.IRB_ID, enroll.SHORT_TITLE " + "from "
			+ "  STAGING_CATISSUE_PAT_ENROLL enroll " + "where "
			+ "  enroll.PART_SOURCE_ID = ? and enroll.PART_SOURCE = ?";

	private final String getMrnDetails = "select " + "  mrn.SITE_NAME, mrn.MRN_VALUE, mrn.NEW_MRN_VALUE " + "from "
			+ "  STAGING_CATISSUE_MRN mrn " + "where "
			+ "  mrn.PART_SOURCE_ID = ? and mrn.PART_SOURCE = ? and mrn.UPDATE_FLAG = 1";

	private final String getMrnDetailsWithoutFlag = "select " + "  mrn.SITE_NAME, mrn.MRN_VALUE, mrn.NEW_MRN_VALUE "
			+ "from " + "  STAGING_CATISSUE_MRN mrn " + "where " + "  mrn.PART_SOURCE_ID = ? and mrn.PART_SOURCE = ?";

	private final String getConsentDetails = "select "
			+ "  CONSENT_STATEMENT,CONSENT_RESPONSE,IRB_ID, SHORT_TITLE, UPDATE_FLAG " + "from "
			+ "  STAGING_CATISSUE_CONSENTS " + "where "
			+ "  PART_SOURCE_ID = ? and PART_SOURCE = ? and UPDATE_FLAG = 1";

	private final String getCpIdByIrbId = "select identifier from catissue_collection_protocol where irb_identifier = ? and short_title = ?";

	private final String getCollectedSpecimenCountByPartId = "select " + "  count(sp.identifier) " + "from "
			+ "  catissue_specimen sp "
			+ "  join catissue_specimen_coll_group scg on scg.identifier = sp.SPECIMEN_COLLECTION_GROUP_ID "
			+ "  join catissue_coll_prot_reg cpr on cpr.identifier = scg.collection_protocol_reg_id "
			+ "where sp.collection_status = 'Collected' and cpr.participant_id = ?";

	private static String updateParticipantAuditTable = "insert into STAGING_PARTICIPANT_AUDIT (PART_SOURCE_ID,UPDATED_ON,STATUS,COMMENTS,PART_SOURCE, CUSTOM_MESSAGE) values(?,?,?,?,?,?)";

	private static String updateCatissueJobAudit = "insert into STAGING_JOB_AUDIT (RUN_ON,ADDED_COUNT,EDITED_COUNT,FAILED_COUNT) values(?,?,?,?)";

	private static String getConsentBasicDetails = "select " + "  con.irb_id, con.short_title " + "from "
			+ "  staging_catissue_consents con " + "where "
			+ " con.PART_SOURCE_ID = ? and con.PART_SOURCE = ? and Update_Flag = 1 "
			+ "group by con.irb_id, con.short_title ";

	private static String getPartIdByMRN = "select mrn.participant_id from catissue_part_medical_id mrn "
			+ "	 join catissue_site site on site.identifier = mrn.site_id "
			+ "where mrn.medical_record_number = ? and site.name = ? and rownum = 1";

}
