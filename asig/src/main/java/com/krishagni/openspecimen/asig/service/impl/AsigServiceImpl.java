
package com.krishagni.openspecimen.asig.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.krishagni.catissueplus.core.administrative.events.SiteDetail;
import com.krishagni.catissueplus.core.administrative.events.UserDetail;
import com.krishagni.catissueplus.core.administrative.services.SiteService;
import com.krishagni.catissueplus.core.administrative.services.UserService;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierResponseDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.openspecimen.asig.dao.AsigDao;
import com.krishagni.openspecimen.asig.events.AsigUserDetail;
import com.krishagni.openspecimen.asig.events.ClinicDetail;
import com.krishagni.openspecimen.asig.events.PatientDetail;
import com.krishagni.openspecimen.asig.service.AsigService;
import com.krishagni.rbac.events.RoleDetail;
import com.krishagni.rbac.events.SubjectRoleDetail;
import com.krishagni.rbac.events.SubjectRoleOp;
import com.krishagni.rbac.events.SubjectRoleOp.OP;
import com.krishagni.rbac.service.RbacService;

public class AsigServiceImpl implements AsigService {
	private static final Logger logger = Logger.getLogger(AsigServiceImpl.class);

	private AsigDao asigDao;
	
	private SiteService siteSvc;

	private UserService userSvc;
	
	private RbacService subRoleSvc;

	private CollectionProtocolRegistrationService cprSvc;

	public void setAsigDao(AsigDao asigDao) {
		this.asigDao = asigDao;
	}

	public void setSiteSvc(SiteService siteSvc) {
		this.siteSvc = siteSvc;
	}

	public void setUserSvc(UserService userSvc) {
		this.userSvc = userSvc;
	}

	public void setSubRoleSvc(RbacService subRoleSvc) {
		this.subRoleSvc = subRoleSvc;
	}

	public void setCprSvc(CollectionProtocolRegistrationService cprSvc) {
		this.cprSvc = cprSvc;
	}

	@Override
	public void updateData() {
		createOrUpdateSites();
		createOrUpdateUsers();
		registerOrUpdateParticipants();
	}

	private void createOrUpdateSites() {
		List<ClinicDetail> clinicResult = getClinics();
		for (ClinicDetail clinic : clinicResult) {
			try {
				createOrUpdateSite(clinic);
			} catch (Exception ex) {
				logger.error("Error while creating or updating site. Asig Clinic Id : " + clinic.getClinicId());
				logger.error(ex.getMessage(), ex);
			}
		}
	}

	@PlusTransactional
	private List<ClinicDetail> getClinics() {
		return asigDao.getAsigClinicDetail();
	}

	@PlusTransactional
	private void createOrUpdateSite(ClinicDetail clinic) {
		SiteDetail siteDetail = new SiteDetail();
		populateSiteDetail(siteDetail, clinic);

		Long osSiteId = (asigDao.getOsSiteId(clinic.getClinicId()));

		if (osSiteId == null) {
			ResponseEvent<SiteDetail> result = siteSvc.createSite(getRequest(siteDetail));
			if (result.isSuccessful()) {
				asigDao.insertSiteMapping(clinic.getClinicId(), result.getPayload().getId());
			} else {
				result.throwErrorIfUnsuccessful();
			}

		} else {
			siteDetail.setId(osSiteId);
			ResponseEvent<SiteDetail> result = siteSvc.updateSite(getRequest(siteDetail));
			result.throwErrorIfUnsuccessful();
		}
	}

	private void createOrUpdateUsers() {
		List<AsigUserDetail> users = getAsigUsers();
		for(AsigUserDetail asigUser : users) {
			try {
				createOrUpdateUser(asigUser);
			} catch (Exception ex) {
				logger.error("Error while creating or updating user. Asig User Id - " + asigUser.getUserId());
				logger.error(ex.getMessage(), ex);
			}
		}
	}

	@PlusTransactional
	private List<AsigUserDetail> getAsigUsers() {
		return asigDao.getAsigUserDetail();
	}

	@PlusTransactional
	private void createOrUpdateUser(AsigUserDetail asigUser) {
		UserDetail user = new UserDetail();
		populateUserDetail(user, asigUser);
		String siteName = (asigDao.getSiteName(asigUser.getClinicId()));
		Long osUserId = (asigDao.getOsUserId(Long.parseLong(asigUser.getUserId())));

		if (osUserId == null) {
			ResponseEvent<UserDetail> userCreateResult = userSvc.createUser(getRequest(user));
			userCreateResult.throwErrorIfUnsuccessful();
			ResponseEvent<SubjectRoleDetail> result = addRole(siteName, userCreateResult);

			if (result.isSuccessful()) {
				asigDao.insertUserMapping(Long.parseLong(asigUser.getUserId()), userCreateResult.getPayload().getId());
			} else {
				logger.error(result.getError().getMessage());
			}

		} else {
			user.setId(osUserId);
			ResponseEvent<UserDetail> userUpdateResult = userSvc.updateUser(getRequest(user));
			userUpdateResult.throwErrorIfUnsuccessful();
			ResponseEvent<SubjectRoleDetail> result = addRole(siteName, userUpdateResult);
		}
	}

	private void registerOrUpdateParticipants() {
			List<PatientDetail> result = getPatients();
		for (PatientDetail patient : result) {
			try {
				registerOrUpdateParticipant(patient);
			} catch (Exception ex) {
				logger.error("Error while registering or updating participant. Asig Patient Id - " + patient.getPatientId());
				logger.error(ex.getMessage(), ex);
			}
		}
	}

	@PlusTransactional
	private List<PatientDetail> getPatients() {
		return asigDao.getAsigPatientDetail();
	}

	@PlusTransactional
	private void registerOrUpdateParticipant(PatientDetail patient) {
		CollectionProtocolRegistrationDetail cpr = new CollectionProtocolRegistrationDetail();
		populateCprDetail(cpr, patient);

		Long osPartId = (asigDao.getOsPatientId(Long.parseLong(patient.getPatientId())));

		if (osPartId == null) {
			// Set registration date only while registering participant.
			Date regDate = patient.getDateOfStatusChange() == null ? Calendar.getInstance().getTime() : patient.getDateOfStatusChange();
			cpr.setRegistrationDate(regDate);
			ResponseEvent<CollectionProtocolRegistrationDetail> registerRes = cprSvc.createRegistration(getRequest(cpr));
			registerRes.throwErrorIfUnsuccessful();

			cpr.getConsentDetails().setCprId(registerRes.getPayload().getId());
			ResponseEvent<ConsentDetail> consentResult = cprSvc.saveConsents(getRequest(cpr.getConsentDetails()));
			consentResult.throwErrorIfUnsuccessful();

			asigDao.insertPatientMapping(Long.parseLong(patient.getPatientId()), registerRes.getPayload().getId());
		} else {
			cpr.setId(osPartId);
			ResponseEvent<CollectionProtocolRegistrationDetail> updateRes = cprSvc.updateRegistration(getRequest(cpr));
			updateRes.throwErrorIfUnsuccessful();

			cpr.getConsentDetails().setCprId(updateRes.getPayload().getId());
			ResponseEvent<ConsentDetail> consentResult = cprSvc.saveConsents(getRequest(cpr.getConsentDetails()));
			consentResult.throwErrorIfUnsuccessful();
		}
	}

	private void populateSiteDetail(SiteDetail siteDetail, ClinicDetail clinicDetail) {
		siteDetail.setName(clinicDetail.getDescription());
		siteDetail.setInstituteName(INSTITUTE_NAME);
		List<UserSummary> coordinators = new ArrayList<UserSummary>();
		UserSummary userSummary = new UserSummary();
		userSummary.setEmailAddress(ADMIN_EMAIL);
		coordinators.add(userSummary);
		siteDetail.setCoordinators(coordinators);
		siteDetail.setType(SITE_TYPE_REPOSITORY);
	}
	
	private void populateUserDetail(UserDetail userDetail, AsigUserDetail asigUserDetail) {
		userDetail.setFirstName(asigUserDetail.getFirstName());
		userDetail.setLastName(asigUserDetail.getLastName());
		userDetail.setEmailAddress(asigUserDetail.getEmailAddress());
		userDetail.setLoginName(asigUserDetail.getLoginName());
		userDetail.setDomainName(LOGIN_DOMAIN);
		userDetail.setInstituteName(INSTITUTE_NAME);
		userDetail.setPhoneNumber(PHONE_NUMBER);
		
	}
	
	private void populateCprDetail(CollectionProtocolRegistrationDetail cpr, PatientDetail asigPart) {
		cpr.setPpid(asigPart.getPatientId());
		cpr.setParticipant(getParticipantDetail(asigPart));
		cpr.setConsentDetails(getConsentDetail(asigPart));
		cpr.setCpTitle(CP_TITLE);
	}

	private ParticipantDetail getParticipantDetail(PatientDetail asigPart) {
		List<PmiDetail> pmis= new ArrayList<PmiDetail>();
		PmiDetail pmi = new PmiDetail();
		//pmi.setMrn(asigPart.getHospitalUr());
		pmi.setSiteName(asigPart.getSiteName());
		pmis.add(pmi);

		ParticipantDetail detail= new ParticipantDetail();
		detail.setPmis(pmis);
		detail.setVitalStatus(getVitalStatus(asigPart.getStatus()));
		detail.setSource(PARTICIPANT_SOURCE);
		detail.setDeathDate(asigPart.getLastContactDate());
		return detail;
	}

	private ConsentDetail getConsentDetail(PatientDetail asigPart) {
		List<ConsentTierResponseDetail> consentTierResponse = new ArrayList<>();
		ConsentTierResponseDetail consentTierRespObj = new ConsentTierResponseDetail();
		if (asigPart.getConsent()) {
			consentTierRespObj.setResponse("Yes");
		} else if (!asigPart.getConsent()) {
			consentTierRespObj.setResponse("No");
		} else {
			consentTierRespObj.setResponse("Not Specified");
		}

		consentTierResponse.add(consentTierRespObj);
		consentTierRespObj.setStatement(CONSENT_TO_USE_BLOOD);

		ConsentDetail consentDetail = new ConsentDetail();
		consentDetail.setResponses(consentTierResponse);
		return consentDetail;
	}

	private String getVitalStatus(int code) {
		switch(code) {
			case 1:
				return "Current";
		 	case 2:
		 		return "Withdrawn";
		 	case 3: 
		 		return "Dead";
			case 186:
				return "Unable to Contact";
		 	default:
		 		return "";
		 }
	}

	private ResponseEvent<SubjectRoleDetail> addRole(String siteName, ResponseEvent<UserDetail> userCreateResult) {
		SubjectRoleDetail subjectRoleDetail = getRoleDetail();
		SiteDetail siteDetail = new SiteDetail();
		siteDetail.setName(siteName);
		subjectRoleDetail.setSite(siteDetail);
		subjectRoleDetail.setSystemRole(true);

		SubjectRoleOp subRoleOp = new SubjectRoleOp();
		subRoleOp.setOp(OP.ADD);
		subRoleOp.setSubjectRole(subjectRoleDetail);
		subRoleOp.setSubjectId(userCreateResult.getPayload().getId());

		ResponseEvent<SubjectRoleDetail> result = subRoleSvc.updateSubjectRole(getRequest(subRoleOp));
		result.throwErrorIfUnsuccessful();
		return result;
	}

	private SubjectRoleDetail getRoleDetail() {
		SubjectRoleDetail subjectRoleDetail = new SubjectRoleDetail();

		RoleDetail roleDetail = new RoleDetail();
		roleDetail.setName(TISSUE_BANKER_ROLE);
		subjectRoleDetail.setRole(roleDetail);

		CollectionProtocolSummary collectionProtocolSummaryDetail = new CollectionProtocolSummary();
		collectionProtocolSummaryDetail.setTitle(CP_TITLE);
		collectionProtocolSummaryDetail.setShortTitle(CP_TITLE);
		subjectRoleDetail.setCollectionProtocol(collectionProtocolSummaryDetail);
		return subjectRoleDetail;
	}

	private <T> RequestEvent<T> getRequest(T payload) {
		return new RequestEvent<T>(payload);
	}

	private static final String PARTICIPANT_SOURCE = "ASIG";

	public static final String CONSENT_TO_USE_BLOOD = "Consent to use blood";

	private static final String LOGIN_DOMAIN = "openspecimen";

	private static final String INSTITUTE_NAME = "i";
    
    private static final String CP_TITLE = "ASIG Collection Protocol";
    
    private static final String PHONE_NUMBER = "+61 404622745";
    
    private static final String ADMIN_EMAIL = "candice.rabusa@svha.org.au";
    
    private static final String TISSUE_BANKER_ROLE = "Tissue Banker";
    
    private static final String SITE_TYPE_REPOSITORY = "Repository";
	
}