package com.krishagni.openspecimen.msk2.services.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.biospecimen.services.VisitService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.domain.ExternalAppId;
import com.krishagni.catissueplus.core.common.domain.MessageLog;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.MessageHandler;
import com.krishagni.catissueplus.core.common.service.MessageLogService;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;
import com.krishagni.catissueplus.core.importer.domain.ImportJob;
import com.krishagni.openspecimen.msk2.events.CarsBiospecimenDetail;
import com.krishagni.openspecimen.msk2.events.CollectionDetail;
import com.krishagni.openspecimen.msk2.events.TimepointDetail;
import com.krishagni.openspecimen.msk2.repository.CarsBiospecimenReader;
import com.krishagni.openspecimen.msk2.repository.impl.CarsBiospecimenReaderImpl;
import com.krishagni.openspecimen.msk2.services.CarsBiospecimenImporter;

public class CarsBiospecimenImporterImpl implements CarsBiospecimenImporter, InitializingBean, MessageHandler {
	private static final Log logger = LogFactory.getLog(CarsBiospecimenImporterImpl.class);

	private static final String EXT_APP_NAME = "CARS";
	
	private CollectionProtocolRegistrationService cprSvc;

	private VisitService visitSvc;

	private SpecimenService specimenSvc;
	
	private DaoFactory daoFactory;

	private MessageLogService msgLogSvc;

	public void setCprSvc(CollectionProtocolRegistrationService cprSvc) {
		this.cprSvc = cprSvc;
	}

	public void setVisitSvc(VisitService visitSvc) {
		this.visitSvc = visitSvc;
	}

	public void setSpecimenSvc(SpecimenService specimenSvc) {
		this.specimenSvc = specimenSvc;
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
	public void onStart() {

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

	@Override
	public void onComplete() {

	}

	@PlusTransactional
	private Long importBiospecimen(MessageLog log, CarsBiospecimenDetail detail) {
		String error = null;
		Long recordId = null;

		try {
			recordId = saveOrUpdateCpr(detail);
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

	private Long saveOrUpdateCpr(CarsBiospecimenDetail detail) {
		CollectionProtocolRegistrationDetail inputCpr = toCpr(detail);
		Participant dbParticipant = getParticipantFromDb(inputCpr.getParticipant().getPmis());
		if (dbParticipant != null) {
			inputCpr.getParticipant().setId(dbParticipant.getId());
		}

		Long cprId = null;
		ExternalAppId extAppId = getOsIdFromExtId(CollectionProtocolRegistration.class, getExtCprId(detail));
		if (extAppId == null) {
			cprId = response(cprSvc.createRegistration(request(inputCpr))).getId();
			saveExtId(CollectionProtocolRegistration.class, getExtCprId(detail), cprId);
		} else {
			CollectionProtocolRegistration existing = getCprFromDb(extAppId.getOsId());
			if (existing == null) {
				// the CPR was deleted from OS DB
				return extAppId.getOsId();
			}

			inputCpr.setId(existing.getId());
			inputCpr.setPpid(existing.getPpid());
			inputCpr.setRegistrationDate(existing.getRegistrationDate());
			cprId = response(cprSvc.updateRegistration(request(inputCpr))).getId();
		}

		saveOrUpdateVisits(cprId, detail);
		return cprId;
	}

	private void saveOrUpdateVisits(Long cprId, CarsBiospecimenDetail detail) {
		for (TimepointDetail timepoint : detail.getTimepoints()) {
			VisitDetail visit = toVisit(cprId, detail, timepoint);

			Long visitId = null;
			ExternalAppId extAppId = getOsIdFromExtId(Visit.class, getExtVisitId(detail, timepoint));
			if (extAppId == null) {
				visitId = response(visitSvc.addVisit(request(visit))).getId();
				saveExtId(Visit.class, getExtVisitId(detail, timepoint), visitId);
			} else {
				Visit existing = getVisitFromDb(extAppId.getOsId());
				if (existing == null) {
					// the visit is deleted from OS DB
					return;
				}

				visit.setId(existing.getId());
				visit.setName(existing.getName());
				visit.setStatus(existing.getStatus());
				visitId = response(visitSvc.patchVisit(request(visit))).getId();
			}

			saveOrUpdateSpecimens(cprId, visitId, detail, timepoint);
		}
	}

	private void saveOrUpdateSpecimens(Long cprId, Long visitId, CarsBiospecimenDetail detail, TimepointDetail timepoint) {
		for (CollectionDetail collection : timepoint.getCollections()) {
			SpecimenDetail specimen = toSpecimen(cprId, visitId, collection);

			ExternalAppId extAppId = getOsIdFromExtId(Specimen.class, getExtSpecimenId(detail, timepoint, collection));
			if (extAppId == null) {
				Long specimenId = response(specimenSvc.createSpecimen(request(specimen))).getId();
				saveExtId(Specimen.class, getExtSpecimenId(detail, timepoint, collection), specimenId);
			} else {
				Specimen existing = getSpecimenFromDb(extAppId.getOsId());
				if (existing == null) {
					// the specimen is deleted from OS database
					return;
				}

				specimen.setId(existing.getId());
				specimen.setStatus(existing.getCollectionStatus());
				response(specimenSvc.updateSpecimen(request(specimen)));
			}
		}
	}

	private CollectionProtocolRegistrationDetail toCpr(CarsBiospecimenDetail detail) {
		CollectionProtocolRegistrationDetail cprDetail = new CollectionProtocolRegistrationDetail();
		cprDetail.setCpShortTitle(detail.getIrbNumber());
		cprDetail.setExternalSubjectId(detail.getPatientStudyId());
		cprDetail.setRegistrationDate(Calendar.getInstance().getTime());
		cprDetail.setSite(detail.getFacility());

		ParticipantDetail participantDetail = new ParticipantDetail();
		participantDetail.setFirstName(detail.getFirstName());
		participantDetail.setMiddleName(detail.getMiddleName());
		participantDetail.setLastName(detail.getLastName());
		participantDetail.setPhiAccess(true);

		PmiDetail pmi = new PmiDetail();
		pmi.setSiteName(detail.getFacility());
		pmi.setMrn(detail.getMrn());
		participantDetail.setPmi(pmi);

		cprDetail.setParticipant(participantDetail);
		return cprDetail;
	}

	private VisitDetail toVisit(Long cprId, CarsBiospecimenDetail detail, TimepointDetail timepoint) {
		VisitDetail visit = new VisitDetail();
		visit.setCprId(cprId);
		visit.setName(timepoint.getName());
		visit.setVisitDate(timepoint.getCreationTime());
		visit.setSite(detail.getFacility());
		visit.setStatus(Visit.VISIT_STATUS_PENDING);

		ExternalAppId eventId = getOsIdFromExtId(CollectionProtocolEvent.class, timepoint.getId());
		if (eventId != null) {
			visit.setEventId(eventId.getOsId());
		}

		return visit;
	}

	private SpecimenDetail toSpecimen(Long cprId, Long visitId, CollectionDetail detail) {
		SpecimenDetail specimen = new SpecimenDetail();
		specimen.setLineage(Specimen.NEW);
		specimen.setStatus(Specimen.PENDING);
		specimen.setCprId(cprId);
		specimen.setVisitId(visitId);
		specimen.setLabel(detail.getName());
		specimen.setCreatedOn(detail.getCreationTime());
		specimen.setComments(detail.getComments());

		Map<String, Object> attrsMap = new HashMap<>();
		attrsMap.put("processed", detail.getProcessed());
		attrsMap.put("shipped", detail.getShipped());

		ExtensionDetail customFields = new ExtensionDetail();
		customFields.setAttrsMap(attrsMap);
		specimen.setExtensionDetail(customFields);

		ExternalAppId srId = getOsIdFromExtId(SpecimenRequirement.class, detail.getId());
		if (srId != null) {
			specimen.setReqId(srId.getOsId());
		}

		return specimen;
	}

	private Participant getParticipantFromDb(List<PmiDetail> pmis) {
		List<Participant> participants = daoFactory.getParticipantDao().getByPmis(pmis);
		return !participants.isEmpty() ? participants.get(0) : null;
	}

	private CollectionProtocolRegistration getCprFromDb(Long cprId) {
		return daoFactory.getCprDao().getById(cprId);
	}

	private Visit getVisitFromDb(Long visitId) {
		return daoFactory.getVisitsDao().getById(visitId);
	}

	private Specimen getSpecimenFromDb(Long specimenId) {
		return daoFactory.getSpecimenDao().getById(specimenId);
	}

	private String getExtCprId(CarsBiospecimenDetail detail) {
		return (detail.getIrbNumber() + "-" + detail.getPatientId()).toLowerCase();
	}

	private String getExtVisitId(CarsBiospecimenDetail detail, TimepointDetail timepoint) {
		return getExtCprId(detail) + "-" + timepoint.getId().toLowerCase();
	}

	private String getExtSpecimenId(CarsBiospecimenDetail detail, TimepointDetail timepoint, CollectionDetail coll) {
		return getExtVisitId(detail, timepoint) + "-" + coll.getId().toLowerCase();
	}

	private void saveExtId(Class<?> klass, String extId, Long osId) {
		ExternalAppId externalAppId = new ExternalAppId();
		externalAppId.setAppName(EXT_APP_NAME);
		externalAppId.setEntityName(klass.getName());
		externalAppId.setExternalId(extId);
		externalAppId.setOsId(osId);
		daoFactory.getExternalAppIdDao().saveOrUpdate(externalAppId);
	}

	private ExternalAppId getOsIdFromExtId(Class<?> klass, String extId) {
		return daoFactory.getExternalAppIdDao().getByExternalId(EXT_APP_NAME, klass.getName(), extId);
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
