
package com.krishagni.openspecimen.epic.dao.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;

import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierResponseDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.DaoFactoryImpl;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.openspecimen.epic.dao.EpicDao;
import com.krishagni.openspecimen.epic.events.CprDetail;
import com.krishagni.openspecimen.epic.events.EpicParticipantDetail;

public class EpicDaoImpl implements EpicDao {

	private DaoFactory daoFactory;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<EpicParticipantDetail> getEpicParticipantDetails() {
		List<Object[]> list = getCurrentSession()
				.createSQLQuery(getEpicParticipantDetail)
				.list();
		
		List<EpicParticipantDetail> result = new ArrayList<EpicParticipantDetail>();
		for (Object[] obj : list) {
			result.add(populateEpicObj(obj));
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Long getOsParticipantId(String sourcePartId, String partSource) {
		List<Object[]> result = getCurrentSession()
				.createSQLQuery(getOsParticipantId)
				.setParameter(0, sourcePartId)
				.setParameter(1, partSource)
				.list();

		return result.isEmpty() ? null : Utility.numberToLong(result.get(0));
	}

	@Override
	public void insertMapping(Long origPartId, String sourcePartId, String partSource) {
		getCurrentSession()
				.createSQLQuery(instertMapping)
				.setParameter(0, origPartId)
				.setParameter(1, sourcePartId)
				.setParameter(2, partSource)
				.executeUpdate();
	}

	@Override
	public void updateMapping(Long origPartId, String sourcePartId, String partSource) {
		getCurrentSession()
				.createSQLQuery(updatingMapping)
				.setParameter(0, sourcePartId)
				.setParameter(1, partSource)
				.executeUpdate();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<String> getRace(String sourcePartId, String partSource) {
		List<Object[]> result = getCurrentSession()
				.createSQLQuery(getRace)
				.setParameter(0, sourcePartId)
				.setParameter(1, partSource)
				.list();

		Set<String> race = new HashSet<String>();
		if (!result.isEmpty()) {
			for (Object object : result) {
				race.add(object.toString());
			}
		}
		return race;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<PmiDetail> getPmis(String sourcePartId, String partSource) {
		List<Object[]> result = getCurrentSession()
				.createSQLQuery(getMrnDetails)
				.setParameter(0, sourcePartId)
				.setParameter(1, partSource)
				.list();

		List<PmiDetail> pmis = new ArrayList<PmiDetail>();
		for (Object[] obj : result) {
			PmiDetail pmi = new PmiDetail();
			pmi.setSiteName(getString(obj[0]));
			pmi.setMrn(obj[1].toString());
			pmis.add(pmi);
		}
		return pmis;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<CprDetail> getCprDetails(String sourcePartId, String partSource) {
		List<Object[]> result = getCurrentSession()
				.createSQLQuery(getCprDetail)
				.setParameter(0, sourcePartId)
				.setParameter(1, partSource)
				.list();

		List<CprDetail> cprDetails = new ArrayList<CprDetail>();
		for (Object[] object : result) {
			CprDetail detail = new CprDetail();
			detail.setRegistrationDate(getDate(object[0]));
			detail.setConsentSignatureDate(getDate(object[1]));
			detail.setIrbID(getString(object[2]));
			detail.setConsentResponseList(getConsentDetails(sourcePartId, partSource, detail.getIrbID()));
			cprDetails.add(detail);
		}
		
		return cprDetails;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ConsentTierResponseDetail> getConsentDetails(String sourcePartId, String partSource, String irbID) {
		List<Object[]> result = getCurrentSession()
				.createSQLQuery(getConsentDetails)
				.setParameter(0, sourcePartId)
				.setParameter(1, partSource)
				.list();
		
		List<ConsentTierResponseDetail> consents = new ArrayList<ConsentTierResponseDetail>();
		for (Object[] object : result) {
			if(irbID.equals(object[2].toString())){
				ConsentTierResponseDetail response = new ConsentTierResponseDetail();
				response.setStatement(getString(object[0]));
				response.setResponse(getString(object[1]));
				consents.add(response);
			}
		}
		return consents;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Long getCpIdByIrbId(String irbId) {
		List<Object> result = getCurrentSession()
				.createSQLQuery(getCpIdByIrbId)
				.setParameter(0, irbId)
				.list();
		
		return result.isEmpty() ? null : Utility.numberToLong(result.get(0));
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public boolean hasSpecimens(Long participantId) {
		List<Object[]> result = getCurrentSession()
				.createSQLQuery(getCollectedSpecimenCountByPartId)
				.setParameter(0, participantId)
				.list();
		
		return result.isEmpty() ? false : true;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Long getCprId(Long participantId, Long cpId) {
		List<Object[]> result = getCurrentSession()
				.createSQLQuery(getCprIdByCpAndParticipantId)
				.setParameter(0, participantId)
				.setParameter(1,cpId)
				.list();
		
		return result.isEmpty() ? null : Utility.numberToLong(result.get(0));
	}
	
	@Override
	public void updateAuditLog(String staginPartIdSource, String epicParticipantId, String successMsg, String errorMsg) {
		getCurrentSession()
			.createSQLQuery(updateParticipantAuditTable)
			.setParameter(0, epicParticipantId)
			.setTimestamp(1,new java.sql.Timestamp(System.currentTimeMillis()))
			.setParameter(2, successMsg)
			.setParameter(3, errorMsg)
			.setParameter(4, staginPartIdSource)
			.executeUpdate();
	}
	
	private Session getCurrentSession() {
		return ((DaoFactoryImpl) daoFactory).getSessionFactory().getCurrentSession();
	}

	private EpicParticipantDetail populateEpicObj(Object[] obj) {
		EpicParticipantDetail epicParticipantDetail = new EpicParticipantDetail();

		epicParticipantDetail.setId(getString(obj[0]));
		epicParticipantDetail.setSource(getString(obj[1]));
		epicParticipantDetail.setOsId(Utility.numberToLong(obj[2]));
		epicParticipantDetail.setChangeType(getString(obj[3]));
		epicParticipantDetail.setOldId(getString(obj[4]));
		epicParticipantDetail.setFirstName(getString(obj[5]));
		epicParticipantDetail.setMiddleName(getString(obj[6]));
		epicParticipantDetail.setLastName(getString(obj[7]));
		epicParticipantDetail.setBirthDate(getDate(obj[8]));
		epicParticipantDetail.setGender(getString(obj[9]));
		epicParticipantDetail.setVitalStatus(getString(obj[10]));
		epicParticipantDetail.setDeathDate(getDate(obj[11]));
		epicParticipantDetail.setEthnicity(getString(obj[15]));

		return epicParticipantDetail;
	}

	private Date getDate(Object object) {
		if (object == null) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(ConfigUtil.getInstance().getDateFmt());
		Date date = null;

		try{
			date = sdf.parse(sdf.format(object));
		} catch (ParseException e){
			System.out.println("Error during date parsing....");
		}
		return date;
	}

	private String getString(Object obj) {
		return obj != null ? obj.toString() : "";
	}

	private final String getEpicParticipantDetail = 
			"select " +
			"  participant.PART_SOURCE_ID,participant.PART_SOURCE,mappinginfo.CATISSUE_PART_ID,history.CHANGE_TYPE, " +
			"  history.PART_SOURCE_ID OLD_PART_SOURCE_ID,participant.FIRST_NAME,participant.MIDDLE_NAME,participant.LAST_NAME, " +
			"  participant.DATE_OF_BIRTH,participant.GENDER,participant.VITAL_STATUS,participant.DEATH_DATE, " +
			"  participant.STATUS,participant.PAT_UPDATE_DATE,participant.UPDATE_FLAG,participant.ETHNICITY " +
			"from " +
			"  STAGING_CATISSUE_PATIENT participant " +
			"  left join STAGING_PART_ID_HISTORY history on history.NEW_PART_SOURCE_ID = participant.PART_SOURCE_ID " +
			"  and history.PART_SOURCE = participant.PART_SOURCE " +
			"  left join STAGING_PART_INFO_MAPPING mappinginfo on " +
			"  (mappinginfo.PART_SOURCE_ID = participant.PART_SOURCE_ID and mappinginfo.PART_SOURCE = participant.PART_SOURCE) " +
			"  left join STAGING_CATISSUE_PAT_ENROLL cpr on cpr.PART_SOURCE_ID = participant.PART_SOURCE_ID " +
			"  left join STAGING_CATISSUE_CONSENTS consent on consent.PART_SOURCE_ID = participant.PART_SOURCE_ID " +
			"where " + 
			"  (participant.UPDATE_FLAG = 1 or consent.UPDATE_FLAG = 1 or cpr.UPDATE_FLAG = 1) " +
			"  group by participant.PART_SOURCE_ID,participant.FIRST_NAME, " +
			"  participant.MIDDLE_NAME,participant.LAST_NAME,participant.DATE_OF_BIRTH,participant.GENDER, " +
			"  participant.VITAL_STATUS,participant.DEATH_DATE,participant.STATUS,participant.PAT_UPDATE_DATE, " +
			"  participant.UPDATE_FLAG,history.PART_SOURCE_ID,mappinginfo.CATISSUE_PART_ID,participant.PART_SOURCE, " +
			"  history.CHANGE_TYPE,participant.ETHNICITY " + " order by participant.PART_SOURCE_ID";

	private final String getOsParticipantId = 
			"select CATISSUE_PART_ID from STAGING_PART_INFO_MAPPING where PART_SOURCE_ID = ? and PART_SOURCE = ?";

	private final String instertMapping = 
			"insert into STAGING_PART_INFO_MAPPING ( CATISSUE_PART_ID , PART_SOURCE_ID , PART_SOURCE) values (?,?,?)";

	private final String updatingMapping = 
			"delete from STAGING_PART_INFO_MAPPING where PART_SOURCE_ID = ? and PART_SOURCE = ? ";

	private final String getRace = 
			"select RACE_VALUE from STAGING_CATISSUE_RACE where PART_SOURCE_ID = ? and PART_SOURCE = ? ";

	private final String getCprDetail = 
			"select " +
			"  enroll.REGISTRATION_DATE, enroll.CONSENT_DATE, enroll.IRB_ID " +
			"from " +
			"  STAGING_CATISSUE_PAT_ENROLL enroll " +
			"where " +
			"  enroll.PART_SOURCE_ID = ? and enroll.PART_SOURCE = ? and enroll.UPDATE_FLAG = 1";

	private final String getMrnDetails = 
			"select " +
			"  mrn.SITE_NAME, mrn.MRN_VALUE " +
			"from " +
			"  STAGING_CATISSUE_MRN mrn " +
			"where " +
			"  mrn.PART_SOURCE_ID = ? and mrn.PART_SOURCE = ? and mrn.UPDATE_FLAG = 1";
	
	private final String getConsentDetails = 
			"select " +
			"  CONSENT_STATEMENT,CONSENT_RESPONSE,IRB_ID " +
			"from " +
			"  STAGING_CATISSUE_CONSENTS " +
			"where " +
			"  PART_SOURCE_ID = ? and PART_SOURCE = ? and UPDATE_FLAG = 1";
	
	private final String getCpIdByIrbId = 
			"select identifier from catissue_collection_protocol where irb_identifier = ?";
	
	private final String getCollectedSpecimenCountByPartId = 
			"select " +
			"  count(sp.identifier) " +
			"from " +
			"  catissue_specimen sp " +
			"  join catissue_specimen_coll_group scg on scg.identifier = sp.SPECIMEN_COLLECTION_GROUP_ID " +
			"  join catissue_coll_prot_reg cpr on cpr.identifier = scg.collection_protocol_reg_id " +
			"where sp.collection_status = 'Collected' and cpr.participant_id = ?";
	
	private final String getCprIdByCpAndParticipantId =
			"select " +
			"  cpr.identifier " +
			"from " +
			"  catissue_coll_prot_reg cpr " +
			"where " +
			"  cpr.participant_id = ? and collection_protocol_id = ?";
	
	private static String updateParticipantAuditTable = 
			"insert into STAGING_PARTICIPANT_AUDIT (PART_SOURCE_ID,UPDATED_ON,STATUS,COMMENTS,PART_SOURCE) values(?,?,?,?,?)";

}
