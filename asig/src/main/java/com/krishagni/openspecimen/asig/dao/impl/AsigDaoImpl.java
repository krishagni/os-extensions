
package com.krishagni.openspecimen.asig.dao.impl;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.DaoFactoryImpl;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.openspecimen.asig.dao.AsigDao;
import com.krishagni.openspecimen.asig.events.AsigUserDetail;
import com.krishagni.openspecimen.asig.events.ClinicDetail;
import com.krishagni.openspecimen.asig.events.PatientDetail;

public class AsigDaoImpl implements AsigDao {
	private static final Logger logger = Logger.getLogger(AsigDataImporterDaoImpl.class);

	private DaoFactory daoFactory;
		
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<PatientDetail> getAsigPatientDetail() {
		List<Object[]> list = getCurrentSession()
			.createSQLQuery(GET_PATIENT_LIST)
				.list();
			
		List<PatientDetail> result = new ArrayList<PatientDetail>();
		for (Object[] obj : list) {
			result.add(populateAsigPatientDetail(obj));
		}
		return result;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<ClinicDetail> getAsigClinicDetail() {
		List<Object[]> list = getCurrentSession()
				.createSQLQuery(GET_CLINIC_LIST)
				.list();
			
		List<ClinicDetail> result = new ArrayList<ClinicDetail>();
		for (Object[] obj : list) {
			result.add(populateAsigClinicObj(obj));
		}
		return result;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<AsigUserDetail> getAsigUserDetail() {
		List<Object[]> list = getCurrentSession()
				.createSQLQuery(GET_USER_LIST)
				.list();
			
		List<AsigUserDetail> result = new ArrayList<AsigUserDetail>();
		for (Object[] obj : list) {
			result.add(populateAsigUserObj(obj));
		}
		return result;
	}
	
	@Override
	public void insertPatientMapping(Long asigId, Long osId) {
		insertMapping(asigId, osId, ENTITY_PARTICIPANT);
	}

	@Override
	public void insertSiteMapping(Long asigId, Long osId) {
		insertMapping(asigId, osId, ENTITY_SITE);
	}

	@Override
	public void insertUserMapping(Long asigId, Long osId) {
		insertMapping(asigId, osId, ENTITY_USER);
	}

	
	@Override
	public Long getOsPatientId(Long asigPatientId) {
		return getOsId(asigPatientId, ENTITY_PARTICIPANT);
	}

	@Override
	public Long getOsSiteId(Long asigSiteId) {
		return getOsId(asigSiteId, ENTITY_SITE);
	}
	
	@Override
	public Long getOsUserId(Long asigUserId) {
		return getOsId(asigUserId, ENTITY_USER);
	}
	
	private void insertMapping(Long AsigId, Long osId, String objType) {
		getCurrentSession()
				.createSQLQuery(INSERT_MAPPING)
				.setParameter(0, AsigId)
				.setParameter(1, osId)
				.setParameter(2, objType)
				.executeUpdate();
	}
	
	@SuppressWarnings("unchecked")
	private Long getOsId(Long asigId, String objType) {
		List<Object> result = getCurrentSession()
				.createSQLQuery(GET_OS_ID)
				.setParameter(0, asigId)
				.setParameter(1, objType)
				.list();
		
		return result.isEmpty() ? null : Utility.numberToLong(result.get(0));
	}
	
	@SuppressWarnings("unchecked")
	public String getSiteName(Long aisgClinicId) {
		List<Object> result = getCurrentSession()
				.createSQLQuery(GET_SITE_NAME)
				.setParameter(0, aisgClinicId)
				.list();
		
		return result.isEmpty() ? null : result.get(0).toString();
	}

	@Override
	public void importData(File file, String entity) {
		String truncateQuery = null, columns = null, tableName = null;
		if (entity.equals("clinic")) {
			truncateQuery = TRUNCATE_CLINIC_TABLE;
			tableName = "ASIG_CLINICS";
			columns = "(IDENTIFIER, NAME)";
		} else if (entity.equals("user")) {
			truncateQuery = TRUNCATE_USER_TABLE;
			tableName = "ASIG_USERS";
			columns = "(IDENTIFIER, CLINIC_ID, FIRST_NAME, LAST_NAME, EMAIL_ADDRESS, LOGIN_NAME)";
		} else if (entity.equals("patient")) {
			truncateQuery = TRUNCATE_PATIENT_TABLE;
			tableName = "ASIG_PATIENTS";
			columns = "(IDENTIFIER, CLINIC_ID, HOSPITAL_URL, STATUS, @CONSENT, @DATE1, @DATE2) " +
				"SET CONSENT=if(char_length(TRIM(@CONSENT))> 0, cast(@CONSENT as signed), NULL), " +
				"DATE_OF_STATUS_CHANGE=nullif(@DATE1, ''), DATE_OF_LAST_CONTACT=nullif(@DATE2, '')";
		}

		getCurrentSession().createSQLQuery(truncateQuery).executeUpdate();

		//.execute(truncateQuery);

		logger.info("File Path " + file.getAbsolutePath().toString());
		String loadQuery = "LOAD DATA LOCAL INFILE '" + file.getAbsolutePath().toString()+
			"' INTO TABLE " + tableName +
			" FIELDS TERMINATED BY ','" +
			" ENCLOSED BY '\"'" +
			" LINES TERMINATED BY '\r\n'" +
			" IGNORE 1 LINES " + columns;

		loadQuery = StringEscapeUtils.escapeJava(loadQuery);

		getCurrentSession().createSQLQuery(loadQuery).executeUpdate();
	}


	private Session getCurrentSession() {
		return ((DaoFactoryImpl) daoFactory).getSessionFactory().getCurrentSession();
	}

	private PatientDetail populateAsigPatientDetail(Object[] obj) {
		PatientDetail asigPatientDetail = new PatientDetail();
		
		asigPatientDetail.setPatientId(getString(obj[0]));
		asigPatientDetail.setClinicId(Long.parseLong(getString(obj[1])));
		//asigPatientDetail.setHospitalUr(getString(obj[2]));
		asigPatientDetail.setStatus(getInteger(obj[3]));
		asigPatientDetail.setConsent((Boolean) obj[4]);
		asigPatientDetail.setSiteName(getString(obj[5]));
		asigPatientDetail.setDateOfStatusChange(getDate(obj[6]));
		asigPatientDetail.setLastContactDate(getDate(obj[7]));
		
		return asigPatientDetail;
	}
	
	private ClinicDetail populateAsigClinicObj(Object[] obj) {
		
		ClinicDetail asigClinicDetail = new ClinicDetail();
		
		asigClinicDetail.setClinicId(Long.parseLong(getString(obj[0])));
		asigClinicDetail.setDescription((getString(obj[1])));
		
		return asigClinicDetail;
	}

	private AsigUserDetail populateAsigUserObj(Object[] obj) {
		AsigUserDetail asigUserDetail = new AsigUserDetail();
		asigUserDetail.setUserId((getString(obj[0])));
		asigUserDetail.setClinicId(Long.parseLong(getString(obj[1])));
		asigUserDetail.setFirstName((getString(obj[2])));
		asigUserDetail.setLastName((getString(obj[3])));
		asigUserDetail.setEmailAddress((getString(obj[4])));
		asigUserDetail.setLoginName((getString(obj[5])));
		
		return asigUserDetail;
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
			System.out.println("Error during date parsing....");
		}
		return date;
	}

	private String getString(Object obj) {
		return obj != null ? obj.toString() : "";
	}
	
	private int getInteger(Object obj) {
		return obj != null ? (Integer) obj : null;
	}

	private static final String ENTITY_PARTICIPANT = "Participant";

	private static final String ENTITY_SITE = "Site";

	private static final String ENTITY_USER = "User";

	private final String GET_PATIENT_LIST  = 
			"select " +
			"  p.identifier, p.clinic_id, p.hospital_url, p.status, p.consent, c.name," +
			"  p.date_of_status_change, p.date_of_last_contact " +
			"from " +
			"  asig_patients p" +
			"  left join asig_clinics c on c.identifier = p.clinic_id ";

	private final String GET_CLINIC_LIST = 
			"select identifier, name from asig_clinics";

	private final String GET_USER_LIST = 
			"select identifier,clinic_id,first_name,last_name,email_address,login_name from asig_users";
		
	private final String INSERT_MAPPING = 
			"insert into object_mapping (src_id ,os_id, object_type) values (?,?,?)";
	
	private final String GET_OS_ID = 
			"select os_id from object_mapping where src_id = ? and object_type = ?";
	
	private final String GET_SITE_NAME = 
			"select name from asig_clinics where identifier = ?";

	private static final String TRUNCATE_CLINIC_TABLE = "truncate table asig_clinics";

	private static final String TRUNCATE_USER_TABLE = "truncate table asig_users";

	private static final String TRUNCATE_PATIENT_TABLE = "truncate table asig_patients";


	
}