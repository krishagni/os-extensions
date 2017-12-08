package com.krishagni.openspecimen.asig.dao.impl;

import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.openspecimen.asig.dao.AsigDataImporterDao;
import com.krishagni.openspecimen.asig.events.AsigUserDetail;
import com.krishagni.openspecimen.asig.events.ClinicDetail;
import com.krishagni.openspecimen.asig.events.PatientDetail;
import com.krishagni.openspecimen.asig.util.DbUtil;

public class AsigDataImporterDaoImpl implements AsigDataImporterDao, InitializingBean {
	private static final Logger logger = Logger.getLogger(AsigDataImporterDaoImpl.class);

	private DataSource dataSource;

	private JdbcTemplate jdbcTemplate;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		InputStream in = null;

		try {
			in = this.getClass().getClassLoader().getResourceAsStream("application.properties");
			Properties props = new Properties();
			props.load(in);
			String pluginDir = props.getProperty("plugin.dir");
			Resource resource = new FileSystemResource(pluginDir + "/asig.properties");
			Properties asigProps = PropertiesLoaderUtils.loadProperties(resource);
			setDataSource(DbUtil.getDataSource(asigProps));
		} catch (Exception e) {
			throw new RuntimeException("Error while creating datasource of ASIG database.", e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	@Override
	public List<ClinicDetail> getClinicDetail() {
		List<ClinicDetail> clinics = null;
		Connection connection = null;
		CallableStatement callableSt = null;
		ResultSet rs = null;

		try {
			connection = jdbcTemplate.getDataSource().getConnection();
			callableSt = connection.prepareCall(GET_CLINIC_DETAILS);
			callableSt.setString(1, null);
			callableSt.execute();
			rs = callableSt.getResultSet();
			clinics = populateAsigClinicDetails(rs);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
			throw OpenSpecimenException.serverError(e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}

				if (callableSt != null) {
					callableSt.close();
				}

				if (connection != null) {
					connection.close();

				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
		return clinics;
	}


	@Override
	public List<AsigUserDetail> getUserDetail() {
		List<AsigUserDetail> users = null;
		Connection connection = null;
		CallableStatement callableSt = null;
		ResultSet rs = null;

		try {
			connection = jdbcTemplate.getDataSource().getConnection();
			callableSt = connection.prepareCall(GET_USER_DETAILS);
			callableSt.setString(1, null);
			callableSt.execute();
			rs = callableSt.getResultSet();
			users = populateAsigUserDetails(rs);
		} catch (SQLException e) {
			throw new OpenSpecimenException(e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}

				if (callableSt != null) {
					callableSt.close();
				}

				if (connection != null) {
					connection.close();

				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
		return users;
	}

	@Override
	public List<PatientDetail> getPatients() {
		List<PatientDetail> patients = null;
		Connection connection = null;
		CallableStatement callableSt = null;
		ResultSet rs = null;

		try {
			connection = jdbcTemplate.getDataSource().getConnection();
			callableSt = connection.prepareCall(GET_PATIENT_DETAILS);
			callableSt.setString(1, null);
			callableSt.execute();
			rs = callableSt.getResultSet();
			patients = populatePatientDetails(rs);
		} catch (SQLException e) {
			throw new OpenSpecimenException(e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}

				if (callableSt != null) {
					callableSt.close();
				}

				if (connection != null) {
					connection.close();

				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
		return patients;
	}

	private List<ClinicDetail> populateAsigClinicDetails(ResultSet rs) {
		List<ClinicDetail> clinicDetails = new ArrayList<>();
		try {
			while (rs.next()) {
				ClinicDetail clinicDetail = new ClinicDetail();
				clinicDetail.setClinicId(rs.getLong(1));
				clinicDetail.setDescription(rs.getString(2));
				clinicDetails.add(clinicDetail);
			}
		} catch (SQLException e) {
			throw new OpenSpecimenException(e);
		}

		return clinicDetails;
	}

	private List<AsigUserDetail> populateAsigUserDetails(ResultSet rs) {
		List<AsigUserDetail> users = new ArrayList<>();
		try {
			while (rs.next()) {
				AsigUserDetail user = new AsigUserDetail();
				user.setUserId(rs.getString("UserID"));
				user.setClinicId(rs.getLong("ClincID"));
				user.setFirstName(rs.getString("GivenName"));
				user.setLastName(rs.getString("Surname"));
				user.setEmailAddress(rs.getString("Email"));
				user.setLoginName(rs.getString("UserName"));
				users.add(user);
			}
		} catch (Exception e) {
			throw new OpenSpecimenException(e);
		}
		return users;
	}

	private List<PatientDetail> populatePatientDetails(ResultSet rs) {
		List<PatientDetail> patients = new ArrayList<>();
		try {
			while (rs.next()) {
				PatientDetail patient = new PatientDetail();
				patient.setPatientId(rs.getString("PatientID"));
				patient.setClinicId(rs.getLong("ClinicID"));
				patient.setHospitalUr(rs.getString("HospitalUR"));
				patient.setStatus(rs.getInt("Status"));
				patient.setConsent(rs.getBoolean("PatientConsent"));
				patient.setDateOfStatusChange(rs.getDate("StatusChangeDate"));
				patient.setLastContactDate(rs.getDate("LastContact"));
				patients.add(patient);
			}
		} catch (Exception e) {
			throw new OpenSpecimenException(e);
		}
		return patients;
	}

	private static final String GET_CLINIC_DETAILS = "{CALL caTissue_Clinic_Export(?)}";

	private static final String GET_USER_DETAILS = "{CALL caTissue_User_Export(?)}";

	private static final String GET_PATIENT_DETAILS = "{CALL caTissue_Patient_Export(?)}";
}
