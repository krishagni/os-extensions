package com.krishagni.openspecimen.cchs.dao.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.DaoFactoryImpl;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.openspecimen.cchs.events.SpecModByParticpantDetail;


public class SpecModByParticpantDaoImpl extends AbstractDao<SpecModByParticpantDetail> implements SpecModByParticpantDao{

	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	protected Session getCurrentSession() {
		return ((DaoFactoryImpl) daoFactory).getSessionFactory().getCurrentSession();
	}

	@PlusTransactional
	@Override
	public List<SpecModByParticpantDetail> getExportDetails(Date startDate, Date endDate) {

		List<Object[]> list = getCurrentSession().
				createSQLQuery(GET_EXPORT_RULES_SQL).
				setDate("startDate", startDate).
				setDate("endDate", endDate).
				list();

		List<SpecModByParticpantDetail> result = new ArrayList<SpecModByParticpantDetail>();

		for(Object[] obj: list) {
		  result.add(populateExportDetail(obj));
		}

		return result;
	}

	@Override
	public Date getDate(Long id) {
		List<Object[]> scheduledJob=getCurrentSession()
				.createSQLQuery(GET_LAST_EXECUTED_JOB)
				.setInteger("ID", Math.toIntExact(id))
				.list();

		Object[] reqJobDetail = scheduledJob.get(0);

		Date date= getDateFromJob(reqJobDetail);
		return  date;
	}

	private Date getDateFromJob(Object[] obj) {
		Date date = getDate(obj[4]);
		return  date;
	}

	private SpecModByParticpantDetail populateExportDetail(Object[] obj) {
		SpecModByParticpantDetail detail = new SpecModByParticpantDetail();

		detail.setSpecimenId(getNumber(obj[0]));
		detail.setParticipantId((getNumber(obj[1])));
		detail.setFirstName(getString(obj[2]));
		detail.setMiddleName(getString(obj[3]));
		detail.setLastName(getString(obj[4]));
		detail.setBirthDate(getDate(obj[5]));

		detail.setMedRecNo(getString(obj[6]));
		detail.setTissueSite(getString(obj[7]));
		detail.setCollectionDate(getDate(obj[8]));
		detail.setGender(getString(obj[9]));
		return detail;
	}

	private String getString(Object obj) {
		return obj != null ? obj.toString() : "";
	}

	private long getNumber(Object obj) {
	   return (long) Integer.valueOf(getString(obj));
	}

	private Date getDate(Object obj) {
		if(obj!=null) {
		  Date date = (Date) obj;
		  return  date;
		}
		else
		  return  null;
	}

	public static final String GET_EXPORT_RULES_SQL =
		"select "+
		  "specimen.identifier SPECIMEN_ID, participant.identifier PARTICIPANT_ID, participant.FIRST_NAME, "+
		  "participant.MIDDLE_NAME, participant.LAST_NAME, participant.BIRTH_DATE, medical.MEDICAL_RECORD_NUMBER, "+
		  "specimen.TISSUE_SITE, specimen.CREATED_ON, participant.GENDER "+
		"from "+
		  "catissue_participant participant "+
	 	  "left join catissue_part_medical_id medical on medical.PARTICIPANT_id = participant.IDENTIFIER "+
		  "join catissue_coll_prot_reg reg on reg.PARTICIPANT_ID = participant.IDENTIFIER "+
		  "join catissue_specimen_coll_group  grp on grp.COLLECTION_PROTOCOL_REG_ID = reg.IDENTIFIER "+
		  "join catissue_specimen specimen on specimen.SPECIMEN_COLLECTION_GROUP_ID = grp.IDENTIFIER "+
		  "join catissue_specimen_aud aud on aud.IDENTIFIER = specimen.IDENTIFIER "+
		  "join os_revisions rev on aud.REV = rev.REV "+
		"where "+
		  "rev.REVTSTMP between :startDate and :endDate ";


	public static final String GET_LAST_EXECUTED_JOB =
	  "select * from os_scheduled_job_runs where STATUS ='SUCCEEDED' AND SCHEDULED_JOB_ID=:ID ORDER BY IDENTIFIER DESC LIMIT 1";
}
