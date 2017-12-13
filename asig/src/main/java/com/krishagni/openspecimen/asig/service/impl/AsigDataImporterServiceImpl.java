package com.krishagni.openspecimen.asig.service.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.openspecimen.asig.dao.AsigDao;
import com.krishagni.openspecimen.asig.dao.AsigDataImporterDao;
import com.krishagni.openspecimen.asig.events.AsigUserDetail;
import com.krishagni.openspecimen.asig.events.ClinicDetail;
import com.krishagni.openspecimen.asig.events.PatientDetail;
import com.krishagni.openspecimen.asig.service.AsigDataImporterService;

public class AsigDataImporterServiceImpl implements AsigDataImporterService {
	private AsigDataImporterDao asigDataImporterDao;

	private AsigDao asigDao;

	public void setAsigDataImporterDao(AsigDataImporterDao asigDataImporterDao) {
		this.asigDataImporterDao = asigDataImporterDao;
	}

	public void setAsigDao(AsigDao asigDao) {
		this.asigDao = asigDao;
	}

	@Override
	public void importAsigData() throws OpenSpecimenException {
		createClinicDetailsCsvFile();
		importCsv(getClinicDetailsFile(), "clinic");

		createUserDetailsCsvFile();
		importCsv(getUserDetailsFile(), "user");

		createPatientsCsvFile();
		importCsv(getPatientDetailsFile(), "patient");
	}

	@PlusTransactional
	private void importCsv(File file, String entity) {
		asigDao.importData(file, entity);
	}

	private void createClinicDetailsCsvFile() {
		List<ClinicDetail> clinics = asigDataImporterDao.getClinicDetail();

		File file = getClinicDetailsFile();

		CsvWriter writer = null;
		try {
			writer = CsvFileWriter.createCsvFileWriter(file);
			writer.writeNext(getClinicsFileHeader());
			for(ClinicDetail clinic : clinics) {
				ArrayList<String> cells = new ArrayList<>();
				cells.add(clinic.getClinicId().toString());
				cells.add(clinic.getDescription());
				writer.writeNext(cells.toArray(new String[0]));
			}
		} catch (Exception ex) {
			throw OpenSpecimenException.serverError(ex);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	private void createUserDetailsCsvFile() {
		List<AsigUserDetail> users = asigDataImporterDao.getUserDetail();

		File file = getUserDetailsFile();
		CsvWriter writer = null;
		try {
			writer = CsvFileWriter.createCsvFileWriter(file);
			writer.writeNext(getUsersFileHeader());
			for(AsigUserDetail user : users) {
				ArrayList<String> cells = new ArrayList<>();
				cells.add(user.getUserId());
				cells.add(getString(user.getClinicId()));
				cells.add(user.getFirstName());
				cells.add(user.getLastName());
				cells.add(user.getEmailAddress());
				cells.add(user.getLoginName());
				writer.writeNext(cells.toArray(new String[0]));
			}
		} catch (Exception ex) {
			throw OpenSpecimenException.serverError(ex);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	private void createPatientsCsvFile() {
		List<PatientDetail> patients = asigDataImporterDao.getPatients();

		File file = getPatientDetailsFile();
		CsvWriter writer = null;
		try {
			writer = CsvFileWriter.createCsvFileWriter(file);
			writer.writeNext(getPatientsFileHeader());
			for(PatientDetail patient : patients) {
				ArrayList<String> cells = new ArrayList<>();
				cells.add(patient.getPatientId());
				cells.add(getString(patient.getClinicId()));
				cells.add(patient.getHospitalUr());
				cells.add(getString(patient.getStatus()));
				Integer consent = getConsent(patient.getConsent());
				cells.add(getString(consent));
				cells.add(getString(patient.getDateOfStatusChange()));
				cells.add(getString(patient.getLastContactDate()));
				writer.writeNext(cells.toArray(new String[0]));
			}
		} catch (Exception ex) {
			throw OpenSpecimenException.serverError(ex);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	private Integer getConsent(Boolean isConsent) {
		Integer consent;
		if (isConsent == null) {
			consent = null;
		} else if (isConsent) {
			consent = 1;
		} else {
			consent = 0;
		}

		return consent;
	}

	private String[] getClinicsFileHeader() {
		return new String[] {"Identifier", "name"};
	}

	private String[] getUsersFileHeader() {
		return new String[] {"Identifier", "Clinic ID", "First Name", "Last Name", "Email Address", "Login Name"};
	}

	private String[] getPatientsFileHeader() {
		return new String[] {"Identifier", "Clinic ID", "Hospital URL", "Status", "Consent", "Status Changed Date", "Last Contact Date"};
	}

	private File getClinicDetailsFile() {
		return new File(getAsigDir(), "clinics.csv");
	}

	private File getUserDetailsFile() {
		return new File(getAsigDir(), "users.csv");
	}

	private File getPatientDetailsFile() {
		return new File(getAsigDir(), "patients.csv");
	}

	private File getAsigDir() {
		File file = new File(ConfigUtil.getInstance().getDataDir(), "asig");
		if (!file.exists()) {
			file.mkdirs();
		}

		return file;
	}

	private String getString(Object obj) {
		return obj != null ? obj.toString() : null;
	}
}
