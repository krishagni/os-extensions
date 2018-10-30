package com.krishagni.openspecimen.msk2.services.impl;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.events.SiteDetail;
import com.krishagni.catissueplus.core.administrative.events.UserDetail;
import com.krishagni.catissueplus.core.administrative.services.SiteService;
import com.krishagni.catissueplus.core.administrative.services.UserService;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolEventDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSiteDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenRequirementDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.domain.ExternalAppId;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.openspecimen.msk2.domain.CarsErrorCode;
import com.krishagni.openspecimen.msk2.domain.CarsStudyImportJob;
import com.krishagni.openspecimen.msk2.events.CarsStudyDetail;
import com.krishagni.openspecimen.msk2.events.CollectionDetail;
import com.krishagni.openspecimen.msk2.events.TimepointDetail;
import com.krishagni.openspecimen.msk2.repository.CarsStudyImportJobDao;
import com.krishagni.openspecimen.msk2.repository.CarsStudyReader;
import com.krishagni.openspecimen.msk2.repository.impl.CarsStudyReaderImpl;
import com.krishagni.openspecimen.msk2.services.CarsStudyImporter;

public class CarsStudyImporterImpl implements CarsStudyImporter {
	private static final Log logger = LogFactory.getLog(CarsStudyImporterImpl.class);

	private CarsStudyImportJobDao importJobDao;

	private DaoFactory daoFactory;

	private UserService userSvc;

	private SiteService siteSvc;

	private CollectionProtocolService cpSvc;

	public void setImportJobDao(CarsStudyImportJobDao importJobDao) {
		this.importJobDao = importJobDao;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setUserSvc(UserService userSvc) {
		this.userSvc = userSvc;
	}

	public void setSiteSvc(SiteService siteSvc) {
		this.siteSvc = siteSvc;
	}

	public void setCpSvc(CollectionProtocolService cpSvc) {
		this.cpSvc = cpSvc;
	}

	@Override
	public void importStudies() {
		Date startTime = Calendar.getInstance().getTime();
		Date lastUpdated = getLastestJobRunDate();

		try {
			CarsStudyReader reader = new CarsStudyReaderImpl(getCarsDbUrl(), getCarsDbUser(), getCarsDbPassword());

			int numStudies = 0;
			CarsStudyDetail study;
			while ((study = reader.next()) != null) {
				importStudy(lastUpdated, study);
				++numStudies;
			}

			saveJobRun(startTime, numStudies);
		} catch (Exception e) {
			logger.error("Error importing CARS studies", e);
		}
	}

	@PlusTransactional
	private Date getLastestJobRunDate() {
		CarsStudyImportJob lastJob = importJobDao.getLatestJob();
		return lastJob != null ? lastJob.getEndTime() : null;
	}

	@PlusTransactional
	private void saveJobRun(Date startTime, int numStudies) {
		CarsStudyImportJob currentJob = new CarsStudyImportJob();
		currentJob.setStartTime(startTime);
		currentJob.setEndTime(Calendar.getInstance().getTime());
		currentJob.setNoOfStudies(numStudies);
		currentJob.setRunBy(AuthUtil.getCurrentUser());
		importJobDao.saveOrUpdate(currentJob);
	}

	@PlusTransactional
	private void importStudy(Date lastUpdated, CarsStudyDetail inputStudy) {
		CollectionProtocol existingCp = getCpFromDb(inputStudy.getIrbNumber());
		if (existingCp == null) {
			createCp(inputStudy);
		} else {
			updateCp(lastUpdated, existingCp, inputStudy);
		}
	}

	private void createCp(CarsStudyDetail inputStudy) {
		Long piId = createUserIfAbsent(inputStudy.getPiAddress());
		createSiteIfAbsent(inputStudy.getFacility());

		UserSummary pi = new UserSummary();
		pi.setId(piId);

		CollectionProtocolDetail cpDetail = new CollectionProtocolDetail();
		cpDetail.setShortTitle(inputStudy.getIrbNumber());
		cpDetail.setTitle(inputStudy.getIrbNumber());
		cpDetail.setPrincipalInvestigator(pi);

		CollectionProtocolSiteDetail cpSite = new CollectionProtocolSiteDetail();
		cpSite.setSiteName(inputStudy.getFacility());
		cpDetail.setCpSites(Collections.singletonList(cpSite));

		cpDetail = response(cpSvc.createCollectionProtocol(request(cpDetail)));
		saveExtId(CollectionProtocol.class, inputStudy.getIrbNumber(), cpDetail.getId());

		for (TimepointDetail timepoint : inputStudy.getTimepoints()) {
			createEvent(cpDetail.getId(), timepoint);
		}
	}

	private void updateCp(Date lastUpdated, CollectionProtocol existingCp, CarsStudyDetail inputStudy) {
		updateCp(existingCp, inputStudy);

		Map<Long, CollectionProtocolEvent> cpEvents = existingCp.getCollectionProtocolEvents().stream()
			.collect(Collectors.toMap(CollectionProtocolEvent::getId, cpe -> cpe));

		for (TimepointDetail timepoint : inputStudy.getTimepoints()) {
			Long eventId = getOsIdFromExtId(CollectionProtocolEvent.class, timepoint.getId());
			if (eventId == null) {
				createEvent(existingCp.getId(), timepoint);
			} else {
				CollectionProtocolEvent existingEvent = cpEvents.remove(eventId);
				updateEvent(lastUpdated, existingEvent, timepoint);
			}
		}

		cpEvents.forEach((eventId, event) -> closeEvent(event));
	}

	private void updateCp(CollectionProtocol existingCp, CarsStudyDetail inputStudy) {
		boolean siteChanged = existingCp.getRepositories().stream()
			.noneMatch(s -> s.getName().equalsIgnoreCase(inputStudy.getFacility()));

		boolean piChanged = !existingCp.getPrincipalInvestigator()
			.getEmailAddress().equalsIgnoreCase(inputStudy.getPiAddress());

		if (!siteChanged && !piChanged) {
			return;
		}

		CollectionProtocolDetail cpDetail = CollectionProtocolDetail.from(existingCp);
		if (siteChanged) {
			createSiteIfAbsent(inputStudy.getFacility());

			CollectionProtocolSiteDetail cpSite = new CollectionProtocolSiteDetail();
			cpSite.setSiteName(inputStudy.getFacility());
			cpDetail.getCpSites().add(cpSite);
		}

		if (piChanged) {
			Long piId = createUserIfAbsent(inputStudy.getPiAddress());

			UserSummary pi = new UserSummary();
			pi.setId(piId);
			cpDetail.setPrincipalInvestigator(pi);
		}

		response(cpSvc.updateCollectionProtocol(request(cpDetail)));
	}

	private void createEvent(Long cpId, TimepointDetail timepoint) {
		CollectionProtocolEventDetail event = toEvent(cpId, timepoint);
		event = response(cpSvc.addEvent(request(event)));
		saveExtId(CollectionProtocolEvent.class, timepoint.getId(), event.getId());

		for (CollectionDetail collection : timepoint.getCollections()) {
			createRequirement(event.getId(), collection);
		}
	}

	private void updateEvent(Date lastUpdated, CollectionProtocolEvent existingEvent, TimepointDetail timepoint) {
		if (lastUpdated == null || lastUpdated.before(timepoint.getUpdateTime())) {
			CollectionProtocolEventDetail event = CollectionProtocolEventDetail.from(existingEvent);
			event.setEventLabel(toEventLabel(timepoint));
			response(cpSvc.updateEvent(request(event)));
		}

		Map<Long, SpecimenRequirement> requirements = existingEvent.getTopLevelAnticipatedSpecimens().stream()
			.collect(Collectors.toMap(SpecimenRequirement::getId, sr -> sr));

		for (CollectionDetail collection : timepoint.getCollections()) {
			Long srId = getOsIdFromExtId(SpecimenRequirement.class, collection.getId());
			if (srId == null) {
				createRequirement(existingEvent.getId(), collection);
			} else {
				SpecimenRequirement existingSr = requirements.remove(srId);
				updateRequirement(lastUpdated, existingSr, collection);
			}
		}

		requirements.forEach((srId, sr) -> closeRequirement(sr));
	}

	private void closeEvent(CollectionProtocolEvent existingEvent) {
		CollectionProtocolEventDetail event = CollectionProtocolEventDetail.from(existingEvent);
		event.setActivityStatus(Status.ACTIVITY_STATUS_CLOSED.getStatus());
		response(cpSvc.updateEvent(request(event)));
	}

	private CollectionProtocolEventDetail toEvent(Long cpId, TimepointDetail timepoint) {
		CollectionProtocolEventDetail event = new CollectionProtocolEventDetail();
		event.setCpId(cpId);
		event.setEventLabel(toEventLabel(timepoint));
		event.setClinicalDiagnosis("Not Specified");
		event.setClinicalStatus("Not Specified");
		return event;
	}

	private String toEventLabel(TimepointDetail timepoint) {
		return timepoint.getCycle() + " " + timepoint.getName();
	}

	private SpecimenRequirementDetail createRequirement(Long eventId, CollectionDetail collection) {
		SpecimenRequirementDetail sr = toSr(eventId, collection);
		sr = response(cpSvc.addSpecimenRequirement(request(sr)));
		saveExtId(SpecimenRequirement.class, collection.getId(), sr.getId());
		return sr;
	}

	private void updateRequirement(Date lastUpdated, SpecimenRequirement existingSr, CollectionDetail collection) {
		if (lastUpdated != null && !lastUpdated.before(collection.getUpdateTime())) {
			return;
		}

		SpecimenRequirementDetail sr = SpecimenRequirementDetail.from(existingSr);
		sr.setName(collection.getName());
		if (!sr.getType().equalsIgnoreCase(collection.getType())) {
			sr.setSpecimenClass(getSpecimenClass(collection.getType()));
			sr.setType(collection.getType());
		}

		sr.setCollectionContainer(collection.getContainer());
		sr.setCollectionProcedure(collection.getProcedure());

		response(cpSvc.updateSpecimenRequirement(request(sr)));
	}

	private void closeRequirement(SpecimenRequirement existingSr) {
		SpecimenRequirementDetail sr = SpecimenRequirementDetail.from(existingSr);
		sr.setActivityStatus(Status.ACTIVITY_STATUS_CLOSED.getStatus());
		response(cpSvc.updateSpecimenRequirement(request(sr)));
	}

	private SpecimenRequirementDetail toSr(Long eventId, CollectionDetail collection) {
		SpecimenRequirementDetail sr = new SpecimenRequirementDetail();
		sr.setEventId(eventId);
		sr.setLineage(Specimen.NEW);
		sr.setName(collection.getName());
		sr.setSpecimenClass(getSpecimenClass(collection.getType()));
		sr.setType(collection.getType());
		sr.setInitialQty(BigDecimal.ZERO);
		sr.setCollectionContainer(collection.getContainer());
		sr.setCollectionProcedure(collection.getProcedure());
		sr.setStorageType("Virtual");
		return sr;
	}

	private String getSpecimenClass(String type) {
		return daoFactory.getPermissibleValueDao().getSpecimenClass(type);
	}

	private Long createUserIfAbsent(String piAddress) {
		User existingUser = daoFactory.getUserDao().getUserByEmailAddress(piAddress);
		return existingUser == null ? createUser(piAddress) : existingUser.getId();
	}

	private Long createUser(String piAddress) {
		UserDetail input = new UserDetail();
		input.setEmailAddress(piAddress);
		input.setDomainName(DEF_DOMAIN);
		input.setInstituteName(DEF_INSTITUTE);

		String[] parts = piAddress.split("@");
		input.setLastName(parts[0]);
		return response(userSvc.createUser(request(input))).getId();
	}

	private Long createSiteIfAbsent(String siteName) {
		Site existingSite = daoFactory.getSiteDao().getSiteByName(siteName);
		return (existingSite == null) ? createSite(siteName) : existingSite.getId();
	}

	private Long createSite(String name) {
		SiteDetail site = new SiteDetail();
		site.setName(name);
		site.setInstituteName(DEF_INSTITUTE);
		site.setType("Collection Site");

		return response(siteSvc.createSite(request(site))).getId();
	}

	private void saveExtId(Class<?> klass, String extId, Long osId) {
		ExternalAppId externalAppId = new ExternalAppId();
		externalAppId.setAppName(EXT_APP_NAME);
		externalAppId.setEntityName(klass.getName());
		externalAppId.setExternalId(extId);
		externalAppId.setOsId(osId);
		daoFactory.getExternalAppIdDao().saveOrUpdate(externalAppId);
	}

	private Long getOsIdFromExtId(Class<?> klass, String extId) {
		ExternalAppId appId = daoFactory.getExternalAppIdDao().getByExternalId(EXT_APP_NAME, klass.getName(), extId);
		return appId != null ? appId.getOsId() : null;
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

	private CollectionProtocol getCpFromDb(String shortTitle) {
		return daoFactory.getCollectionProtocolDao().getCpByShortTitle(shortTitle);
	}

	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	private static final String MODULE = "mskcc2";

	private static final String EXT_APP_NAME = "CARS";

	private static final String DB_URL = "cars_db_url";

	private static final String DB_USER = "cars_db_user";

	private static final String DB_PASSWD = "cars_db_password";

	private static final String DEF_INSTITUTE = "MSKCC";

	private static final String DEF_DOMAIN = "openspecimen";
}
