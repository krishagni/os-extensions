package com.krishagni.openspecimen.cchs.dao.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Session;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.DaoFactoryImpl;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.openspecimen.cchs.dao.SpecModByParticpantDao;
import com.krishagni.openspecimen.cchs.events.SpecModByParticpantDetail;


public class SpecModByParticpantDaoImpl implements SpecModByParticpantDao {
	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	protected Session getCurrentSession() {
		return ((DaoFactoryImpl) daoFactory).getSessionFactory().getCurrentSession();
	}

	@Override
	public List<SpecModByParticpantDetail> getSpecModByParticipantDetails(Date startDate, Date endDate) {

		List<Object[]> list = getCurrentSession().createSQLQuery(GET_SPEC_MOD_BY_PARTICIPANTS)
				.setTimestamp("startDate", startDate)
				.setTimestamp("endDate", endDate)
				.list();

		return list.stream().map(obj -> populateExportDetail(obj)).collect(Collectors.toList());
	}

	@Override
	public Date lastJobRunDateFromJobId(Long jobId) {
		List<Object> scheduledJob=getCurrentSession().createSQLQuery(GET_LAST_JOB_RUN_DATE)
				.setLong("id", jobId)
				.list();

		if(scheduledJob.size()==0)
		{
			Date today = Calendar.getInstance().getTime();
			Calendar cal = Calendar.getInstance();
			cal.setTime(today);
			cal.add(Calendar.DAY_OF_YEAR, -7);
			return Utility.chopTime(cal.getTime());
		}

		Date lastJobRunDate= getDate(scheduledJob.get(0));
		return  lastJobRunDate;
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

	private Long getNumber(Object obj) {
	   return  Long.valueOf(getString(obj));
	}

	private Date getDate(Object obj) {
		if(obj!=null) {
		  Date date = (Date) obj;
		  return  date;
		}

		return  null;
	}

	public static final String GET_SPEC_MOD_BY_PARTICIPANTS =
		"select "+
		  "specimen.IDENTIFIER SPECIMEN_ID, participant.IDENTIFIER PARTICIPANT_ID, participant.FIRST_NAME, "+
		  "participant.MIDDLE_NAME, participant.LAST_NAME, participant.BIRTH_DATE, medical.MEDICAL_RECORD_NUMBER, "+
		  "specimen.TISSUE_SITE, specimen.CREATED_ON, participant.GENDER "+
		"from "+
		  "CATISSUE_PARTICIPANT participant "+
	 	  "left join CATISSUE_PART_MEDICAL_ID medical on medical.PARTICIPANT_ID = participant.IDENTIFIER "+
		  "join CATISSUE_COLL_PROT_REG reg on reg.PARTICIPANT_ID = participant.IDENTIFIER "+
		  "join CATISSUE_SPECIMEN_COLL_GROUP  grp on grp.COLLECTION_PROTOCOL_REG_ID = reg.IDENTIFIER "+
		  "join CATISSUE_SPECIMEN specimen on specimen.SPECIMEN_COLLECTION_GROUP_ID = grp.IDENTIFIER "+
		  "join CATISSUE_SPECIMEN_AUD aud on aud.IDENTIFIER = specimen.IDENTIFIER "+
		  "join OS_REVISIONS rev on aud.REV = rev.REV "+
		"where "+
		  "rev.REVTSTMP between :startDate and :endDate ";


	public static final String GET_LAST_JOB_RUN_DATE =
		"select FINISHED_AT from OS_SCHEDULED_JOB_RUNS where STATUS ='SUCCEEDED' and SCHEDULED_JOB_ID=:id order by IDENTIFIER desc limit 1";
}
