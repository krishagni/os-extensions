package com.krishagni.openspecimen.msk2.services.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.domain.MessageLog;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.MessageHandler;
import com.krishagni.catissueplus.core.common.service.MessageLogService;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.importer.domain.ImportJob;
import com.krishagni.openspecimen.msk2.events.CarsBiospecimenDetail;
import com.krishagni.openspecimen.msk2.repository.CarsBiospecimenReader;
import com.krishagni.openspecimen.msk2.repository.impl.CarsBiospecimenReaderImpl;
import com.krishagni.openspecimen.msk2.services.CarsBiospecimenImporter;

public class CarsBiospecimenImporterImpl implements CarsBiospecimenImporter, InitializingBean, MessageHandler {
	private static final Log logger = LogFactory.getLog(CarsBiospecimenImporterImpl.class);
	
	private CollectionProtocolRegistrationService cprSvc;
	
	private DaoFactory daoFactory;

	private MessageLogService msgLogSvc;

	public void setCprSvc(CollectionProtocolRegistrationService cprSvc) {
		this.cprSvc = cprSvc;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setMsgLogSvc(MessageLogService msgLogSvc) {
		this.msgLogSvc = msgLogSvc;
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
			reader = getBiospecimenReader(lastUpdated);
			
			CarsBiospecimenDetail biospecimen;
			while ((biospecimen = reader.next()) != null) {
				++totalRecords;
				Long recordId = importBiospecimen(null, biospecimen);
				if (currRunLastUpdated == null || (biospecimen.getLastUpdated() != null && biospecimen.getLastUpdated().after(currRunLastUpdated))) {
					currRunLastUpdated = biospecimen.getLastUpdated();
				}

				if (recordId == null) {
					++failedRecords;
				}
			}
		} catch (Exception e) {
			logger.error("Error importing CARS participants", e);
		} finally {
			IOUtils.closeQuietly(reader);
			CarsImportJobUtil.getInstance().finishJob(importJob, totalRecords, failedRecords, currRunLastUpdated);
		}
	}

	@Override
	public String process(MessageLog log) {
		try {
			CarsBiospecimenDetail input = new ObjectMapper().readValue(log.getMessage(), CarsBiospecimenDetail.class);
			Long recordId = importBiospecimen(log, input);
			return recordId != null ? recordId.toString() : null;
		} catch (Exception e) {
			throw new RuntimeException("Error processing the CARS biospecimen record message: " + log.getMessage(), e);
		}
	}

	@PlusTransactional
	private Long importBiospecimen(MessageLog log, CarsBiospecimenDetail detail) {
		String error = null;
		Long recordId = null;

		try {
			recordId = createCpr(detail);
		} catch (Throwable t) {
			logger.error("Error while importing a participant:", t);
			error = Utility.getErrorMessage(t);
		} finally {
			logMessage(detail, log, recordId, error);
		}

		return recordId;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		msgLogSvc.registerHandler("CARS", this);
	}

	private CarsBiospecimenReader getBiospecimenReader(Date lastUpdated) {
		return new CarsBiospecimenReaderImpl(
			CarsImportJobUtil.getInstance().getDbUrl(),
			CarsImportJobUtil.getInstance().getDbUser(),
			CarsImportJobUtil.getInstance().getDbPassword(),
			lastUpdated
		);
	}
	
	private Long createCpr(CarsBiospecimenDetail detail) {
		CollectionProtocolRegistrationDetail cprDetail = toCpr(detail);
		Participant dbParticipant = getParticipantFromDb(cprDetail.getParticipant().getPmis());
		if (dbParticipant != null) {
			cprDetail.getParticipant().setId(dbParticipant.getId());
		}
        
		CollectionProtocolRegistration dbCpr = getCprFromDb(cprDetail);
		if (dbCpr != null) {
			cprDetail.setId(dbCpr.getId());
			return response(cprSvc.updateRegistration(request(cprDetail))).getId();
		} else {
			return response(cprSvc.createRegistration(request(cprDetail))).getId();
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

	private void logMessage(CarsBiospecimenDetail input, MessageLog log, Long recordId, String error) {
		Date currentTime = Calendar.getInstance().getTime();
		if (log == null) {
			log = new MessageLog();
			log.setExternalApp("CARS");
			log.setType("biospecimen");
			log.setMessage(toString(input));
			log.setReceiveTime(currentTime);
		}

		log.setRecordId(recordId != null ? recordId.toString() : null);
		log.setStatus(StringUtils.isBlank(error) ? MessageLog.Status.SUCCESS : MessageLog.Status.FAIL);
		log.setProcessStatus(recordId != null ? MessageLog.ProcessStatus.PROCESSED : MessageLog.ProcessStatus.PENDING);
		log.setProcessTime(currentTime);
		log.setError(error);

		if (log.getId() == null) {
			daoFactory.getMessageLogDao().saveOrUpdate(log);
		}
	}

	private String toString(CarsBiospecimenDetail detail) {
		try {
			return new ObjectMapper().writeValueAsString(detail);
		} catch (Exception e) {
			throw new RuntimeException("Error converting CARS biospecimen record to JSON", e);
		}

	}

	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	private static final String IMPORTER_NAME = "mskcc2_cars_biospecimen_importer";
}
