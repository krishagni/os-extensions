
package com.krishagni.openspecimen.asig.dao.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;

import com.krishagni.openspecimen.asig.events.PatientDetail;
import com.krishagni.openspecimen.asig.events.ClinicDetail;
import com.krishagni.openspecimen.asig.events.AsigUserDetail;
import com.krishagni.openspecimen.asig.dao.AsigDao;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.DaoFactoryImpl;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public class AsigDaoImpl implements AsigDao{

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
			result.add(populateAsigpatientObj(obj));
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
	public void insertPatientMapping(Long AsigId, Long osId) {
		insertMapping(AsigId, osId, "Patient");
	}

	@Override
	public void insertSiteMapping(Long AsigId, Long osId) {
		insertMapping(AsigId, osId, "Site");
	}

	@Override
	public void insertUserMapping(Long AsigId, Long osId) {
		insertMapping(AsigId, osId, "User");
	}

	
	@Override
	public Long getOsPatientId(Long asigPatientId) {
		return getOsId(asigPatientId, "Patient");
	}

	@Override
	public Long getOsSiteId(Long asigSiteId) {
		return getOsId(asigSiteId, "Site");
	}
	
	@Override
	public Long getOsUserId(Long asigUserId) {
		return getOsId(asigUserId, "User");
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
	
	private Session getCurrentSession() {
		return ((DaoFactoryImpl) daoFactory).getSessionFactory().getCurrentSession();
	}

	private PatientDetail populateAsigpatientObj(Object[] obj) {
		
		PatientDetail asigPatientDetail = new PatientDetail();
		
		asigPatientDetail.setPatientId(getString(obj[0]));
		asigPatientDetail.setClinicId(Long.parseLong(getString(obj[1])));
		asigPatientDetail.setHospitalUr(getString(obj[2]));
		asigPatientDetail.setStatus(getInteger(obj[3]));
		asigPatientDetail.setConsent((boolean)obj[4]);
		asigPatientDetail.setSiteName(getString(obj[5]));
		asigPatientDetail.setDateOfStatusChange(getDate(obj[6]));
		
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

	private final String GET_PATIENT_LIST  = 
			"select " +
			"  p.patient_id as patient_id, p.clinic_id as clinic_id, p.hospital_url as hospital_url, " + 
			"  p.status as status, p.patient_consent as patient_consent, c.site_name as site_name, " +
			"  p.date_of_status_change as date_of_status_change " +
			"from " +
			"  patient_export p" +
			"  left join clinic_export c on c.clinic_id = p.clinic_id ";

	private final String GET_CLINIC_LIST = 
			"select * from clinic_export";

	private final String GET_USER_LIST = 
			"select * from user_export";
		
	private final String INSERT_MAPPING = 
			"insert into ASIG_OS_MAPPING ( ASIG_ID , OS_ID, object_type) values (?,?,?)";
	
	private final String GET_OS_ID = 
			"select OS_ID from ASIG_OS_MAPPING where ASIG_ID = ? and object_type = ?";
	
	private final String GET_SITE_NAME = 
			"select site_name from clinic_export where clinic_id = ?";
	
}