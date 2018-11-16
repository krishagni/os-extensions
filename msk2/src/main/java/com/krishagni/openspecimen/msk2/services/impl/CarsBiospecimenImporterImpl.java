package com.krishagni.openspecimen.msk2.services.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.importer.domain.ImportJob;
import com.krishagni.openspecimen.msk2.domain.CarsErrorCode;
import com.krishagni.openspecimen.msk2.events.CarsBiospecimenDetail;
import com.krishagni.openspecimen.msk2.repository.CarsBiospecimenReader;
import com.krishagni.openspecimen.msk2.repository.impl.CarsBiospecimenReaderImpl;
import com.krishagni.openspecimen.msk2.services.CarsBiospecimenImporter;

public class CarsBiospecimenImporterImpl implements CarsBiospecimenImporter {
	private static final Log logger = LogFactory.getLog(CarsBiospecimenImporterImpl.class);
	
	private CollectionProtocolRegistrationService cprSvc;
	
	private DaoFactory daoFactory;

	public void setCprSvc(CollectionProtocolRegistrationService cprSvc) {
		this.cprSvc = cprSvc;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void importParticipants() {
		ImportJob latestJob = CarsImportJobUtil.getInstance().getLatestJob(IMPORTER_NAME);
		if (latestJob != null && latestJob.isInProgress()) {
			logger.error("A prior submitted CARS biospecimen importer job is in progress: " + latestJob.getId());
			return;
		}

		Date lastUpdated = CarsImportJobUtil.getInstance().getLastUpdated(latestJob);
		if (lastUpdated == null) {
			lastUpdated = CarsImportJobUtil.getInstance().getLastUpdated(IMPORTER_NAME);
		}

		ImportJob importJob = CarsImportJobUtil.getInstance().createJob(IMPORTER_NAME);

		Date currRunLastUpdated = null;
		int totalRecords = 0, failedRecords = 0;
		CarsBiospecimenReader reader = null;
		try {
			reader = new CarsBiospecimenReaderImpl(getCarsDbUrl(), getCarsDbUser(), getCarsDbPassword(), lastUpdated);
			
			CarsBiospecimenDetail biospecimen;
			while ((biospecimen = reader.next()) != null) {
				++totalRecords;

				try {
					importBiospecimen(biospecimen);
				} catch (OpenSpecimenException ose) {
					logger.error("Error while importing a participant:", ose);
					++failedRecords;
				} finally {
					if (currRunLastUpdated == null || (biospecimen.getLastUpdated() != null && biospecimen.getLastUpdated().after(currRunLastUpdated))) {
						currRunLastUpdated = biospecimen.getLastUpdated();
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error importing CARS participants", e);
		} finally {
			IOUtils.closeQuietly(reader);
			CarsImportJobUtil.getInstance().finishJob(importJob, totalRecords, failedRecords, currRunLastUpdated);
		}
	}

	@PlusTransactional
	private void importBiospecimen(CarsBiospecimenDetail detail) {
		createCpr(detail);
	}
	
	private void createCpr(CarsBiospecimenDetail detail) {
		CollectionProtocolRegistrationDetail cprDetail = toCpr(detail);
		Participant dbParticipant = getParticipantFromDb(cprDetail.getParticipant().getPmis());
		if (dbParticipant != null) {
			cprDetail.getParticipant().setId(dbParticipant.getId());
		}
        
		CollectionProtocolRegistration dbCpr = getCprFromDb(cprDetail);
		if (dbCpr != null) {
			cprDetail.setId(dbCpr.getId());
			response(cprSvc.updateRegistration(request(cprDetail)));
		} else {
			response(cprSvc.createRegistration(request(cprDetail)));
		}
	}
	
	private CollectionProtocolRegistrationDetail toCpr(CarsBiospecimenDetail detail) {
		CollectionProtocolRegistrationDetail cprDetail = new CollectionProtocolRegistrationDetail();
		cprDetail.setCpShortTitle(detail.getIrbNumber());
		cprDetail.setPpid(detail.getTreatment());
		cprDetail.setRegistrationDate(Calendar.getInstance().getTime());
		cprDetail.setSite(detail.getFacility());

		ParticipantDetail participantDetail = new ParticipantDetail();
		participantDetail.setFirstName(detail.getFirstName());
		participantDetail.setLastName(detail.getLastName());
		participantDetail.setPhiAccess(true);

		PmiDetail pmi = new PmiDetail();
		pmi.setSiteName(detail.getFacility());
		pmi.setMrn(detail.getMrn());
		participantDetail.setPmi(pmi);


		cprDetail.setParticipant(participantDetail);
		return cprDetail;
	}

	private Participant getParticipantFromDb(List<PmiDetail> pmis) {
		List<Participant> participants = daoFactory.getParticipantDao().getByPmis(pmis);
		return !participants.isEmpty() ? participants.get(0) : null;
	}

	private CollectionProtocolRegistration getCprFromDb(CollectionProtocolRegistrationDetail cprDetail) {
		return daoFactory.getCprDao().getCprByCpShortTitleAndPpid(cprDetail.getCpShortTitle(), cprDetail.getPpid());
	}
	
	private String getCarsDbUrl() {
		return getConfigSetting(DB_URL, CarsErrorCode.DB_URL_REQ);
	}

	private String getCarsDbUser() {
		return getConfigSetting(DB_USER, CarsErrorCode.DB_USERNAME_REQ);
	}

	private String getCarsDbPassword() {
		return getConfigSetting(DB_PASSWD, CarsErrorCode.DB_PASSWORD_REQ);
	}

	private String getConfigSetting(String name, ErrorCode errorCode) {
		String result = ConfigUtil.getInstance().getStrSetting(MODULE, name, null);
		if (StringUtils.isBlank(result)) {
			throw OpenSpecimenException.userError(errorCode);
		}

		return result;
	}

	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	private static final String IMPORTER_NAME = "msk2_cars_biospecimen_importer";

	private static final String MODULE = "mskcc2";

	private static final String DB_URL = "cars_db_url";

	private static final String DB_USER = "cars_db_username";

	private static final String DB_PASSWD = "cars_db_password";
}
