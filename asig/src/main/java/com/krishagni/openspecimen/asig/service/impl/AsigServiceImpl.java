
package com.krishagni.openspecimen.asig.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.krishagni.catissueplus.core.administrative.events.SiteDetail;
import com.krishagni.catissueplus.core.administrative.events.UserDetail;
import com.krishagni.catissueplus.core.administrative.services.SiteService;
import com.krishagni.catissueplus.core.administrative.services.UserService;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
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
import com.krishagni.openspecimen.asig.events.ClinicDetail;
import com.krishagni.openspecimen.asig.events.PatientDetail;
import com.krishagni.openspecimen.asig.events.AsigUserDetail;
import com.krishagni.openspecimen.asig.service.AsigService;
import com.krishagni.rbac.events.RoleDetail;//
import com.krishagni.rbac.events.SubjectRoleDetail;
import com.krishagni.rbac.events.SubjectRoleOp;
import com.krishagni.rbac.events.SubjectRoleOp.OP;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.rbac.service.RbacService;

public class AsigServiceImpl implements AsigService {
	
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
	@PlusTransactional
	public void createSites() {
		
		List<ClinicDetail> clinicResult = asigDao.getAsigClinicDetail();
		for(ClinicDetail asigClinicDetail : clinicResult) {
			SiteDetail siteDetail = new SiteDetail();
			populateSiteDetail(siteDetail, asigClinicDetail, false);
			
			Long osSiteId = (asigDao.getOsSiteId(asigClinicDetail.getClinicId()));
			
			if(osSiteId == null) {
				ResponseEvent<SiteDetail> siteCreateResult = siteSvc.createSite(getRequest(siteDetail));
				siteCreateResult.throwErrorIfUnsuccessful();
				asigDao.insertSiteMapping(asigClinicDetail.getClinicId(), siteCreateResult.getPayload().getId());
			} else {
				siteDetail.setId(osSiteId);
				ResponseEvent<SiteDetail> siteUpdateResult = siteSvc.updateSite(getRequest(siteDetail));
				siteUpdateResult.throwErrorIfUnsuccessful();
			}
		}
	}

	@Override
	@PlusTransactional
	public void createUsers() {
		
		List<AsigUserDetail> userResult = asigDao.getAsigUserDetail();
		for(AsigUserDetail asigUserDetail : userResult) {
			
			UserDetail userDetail = new UserDetail();
			populateUserDetail(userDetail, asigUserDetail, false);
			String siteName = (asigDao.getSiteName(asigUserDetail.getClinicId()));
			Long osUserId = (asigDao.getOsUserId(Long.parseLong(asigUserDetail.getUserId())));
			
			if(osUserId==null) {
				ResponseEvent<UserDetail> userCreateResult = userSvc.createUser(getRequest(userDetail));
				userCreateResult.throwErrorIfUnsuccessful();
				
				SubjectRoleDetail subjectRoleDetail = new SubjectRoleDetail();
				
				RoleDetail roleDetail = new RoleDetail();
				roleDetail.setName("Tissue Banker");
				subjectRoleDetail.setRole(roleDetail);
				
				CollectionProtocolSummary  collectionProtocolSummaryDetail = new CollectionProtocolSummary();
				collectionProtocolSummaryDetail.setTitle("ASIG");
				collectionProtocolSummaryDetail.setShortTitle("ASIG");
				subjectRoleDetail.setCollectionProtocol(collectionProtocolSummaryDetail);
				
				SiteDetail siteDetail = new SiteDetail();
				siteDetail.setName(siteName);
				subjectRoleDetail.setSite(siteDetail);
				
				subjectRoleDetail.setSystemRole(true);
				
				SubjectRoleOp subRoleOp = new SubjectRoleOp();
				subRoleOp.setOp(OP.ADD);
				subRoleOp.setSubjectRole(subjectRoleDetail);
				subRoleOp.setSubjectId(userCreateResult.getPayload().getId());
				
				ResponseEvent<SubjectRoleDetail> saveRoleResult = subRoleSvc.updateSubjectRole(getRequest(subRoleOp));
				saveRoleResult.throwErrorIfUnsuccessful();
			
				asigDao.insertUserMapping(Long.parseLong(asigUserDetail.getUserId()), userCreateResult.getPayload().getId());
				
			} else {
				userDetail.setId(osUserId);
				ResponseEvent<UserDetail> userUpdateResult = userSvc.updateUser(getRequest(userDetail));
				userUpdateResult.throwErrorIfUnsuccessful();
				
				SubjectRoleDetail subjectRoleDetail = new SubjectRoleDetail();
				RoleDetail roleDetail = new RoleDetail();
				roleDetail.setName(TISSUE_BANKER_ROLE);
				subjectRoleDetail.setRole(roleDetail);
				
				CollectionProtocolSummary  collectionProtocolSummaryDetail = new CollectionProtocolSummary();
				collectionProtocolSummaryDetail.setTitle(CP_TITLE);
				collectionProtocolSummaryDetail.setShortTitle(CP_TITLE);
				subjectRoleDetail.setCollectionProtocol(collectionProtocolSummaryDetail);
				
				SiteDetail siteDetail = new SiteDetail();
				siteDetail.setName(siteName);
				subjectRoleDetail.setSite(siteDetail);
				
				subjectRoleDetail.setSystemRole(true);
				
				SubjectRoleOp subRoleOp = new SubjectRoleOp();
				subRoleOp.setOp(OP.ADD);
				subRoleOp.setSubjectRole(subjectRoleDetail);
				subRoleOp.setSubjectId(userUpdateResult.getPayload().getId());
				
				ResponseEvent<SubjectRoleDetail> saveRoleResult = subRoleSvc.updateSubjectRole(getRequest(subRoleOp));
				saveRoleResult.throwErrorIfUnsuccessful();
			}
		}
	}
	
	@Override
	@PlusTransactional
	public void registerPatients() {
		
		List<PatientDetail> result = asigDao.getAsigPatientDetail();
		for(PatientDetail asigDetail : result) {
			
			CollectionProtocolRegistrationDetail detail = new CollectionProtocolRegistrationDetail();
			populatePatientDetail(detail, asigDetail, false);
	
			Long osPartId = (asigDao.getOsPatientId(Long.parseLong(asigDetail.getPatientId())));
			
			if(osPartId==null) {
				ResponseEvent<CollectionProtocolRegistrationDetail> registerRes = cprSvc.createRegistration(getRequest(detail));
				registerRes.throwErrorIfUnsuccessful();
				
				detail.getConsentDetails().setCprId(registerRes.getPayload().getId());
				ResponseEvent<ConsentDetail> consentResult = cprSvc.saveConsents(getRequest(detail.getConsentDetails()));
				consentResult.throwErrorIfUnsuccessful();

				asigDao.insertPatientMapping(Long.parseLong(asigDetail.getPatientId()), registerRes.getPayload().getId());
			} else {
				detail.setId(osPartId);
				ResponseEvent<CollectionProtocolRegistrationDetail> updateRes = cprSvc.updateRegistration(getRequest(detail));
				updateRes.throwErrorIfUnsuccessful();
				
				detail.getConsentDetails().setCprId(updateRes.getPayload().getId());
				ResponseEvent<ConsentDetail> consentResult = cprSvc.saveConsents(getRequest(detail.getConsentDetails()));
				consentResult.throwErrorIfUnsuccessful();
			}
		}
	}
	
	private void populateSiteDetail(SiteDetail siteDetail, ClinicDetail clinicDetail,
			boolean isOldObj) {
		
		siteDetail.setName(clinicDetail.getDescription());
		siteDetail.setInstituteName(INSTITUTE_NAME);
		List<UserSummary> coordinators = new ArrayList<UserSummary>();
		UserSummary userSummary = new UserSummary();
		userSummary.setEmailAddress(ADMIN_EMAIL);
		coordinators.add(userSummary);
		siteDetail.setCoordinators(coordinators);
		siteDetail.setType(SITE_TYPE_REPOSITORY);
	}
	
	private void populateUserDetail(UserDetail userDetail, AsigUserDetail asigUserDetail,
			boolean isOldObj) {
		
		userDetail.setFirstName(asigUserDetail.getFirstName());
		userDetail.setLastName(asigUserDetail.getLastName());
		userDetail.setEmailAddress(asigUserDetail.getEmailAddress());
		userDetail.setLoginName(asigUserDetail.getLoginName());
		userDetail.setDomainName("openspecimen");
		userDetail.setInstituteName(INSTITUTE_NAME);
		userDetail.setPhoneNumber(PHONE_NUMBER);
		
	}
	
	private void populatePatientDetail(CollectionProtocolRegistrationDetail cprDetail, PatientDetail asigPart,
			boolean isOldObj) {
		
		cprDetail.setPpid(asigPart.getPatientId());
		ParticipantDetail partDetail= new ParticipantDetail();
		List<PmiDetail> pms= new ArrayList<PmiDetail>();
		PmiDetail objpmi = new PmiDetail();
		objpmi.setMrn(asigPart.getHospitalUr());
		objpmi.setSiteName(asigPart.getSiteName());
		pms.add(objpmi);
		partDetail.setPmis(pms);
		partDetail.setVitalStatus(getVitalStatus(asigPart.getStatus()));
		cprDetail.setParticipant(partDetail);
		ConsentDetail consentDetail = new ConsentDetail();
		List<ConsentTierResponseDetail> consentTierResponse = new ArrayList<ConsentTierResponseDetail>();
		ConsentTierResponseDetail consentTierRespObj = new ConsentTierResponseDetail();
		
		if(asigPart.getConsent()==true) {
			consentTierRespObj.setResponse("yes");
		} else if(asigPart.getConsent()==false) {
			consentTierRespObj.setResponse("no");
		} else {
			consentTierRespObj.setResponse("Not Specified");
		}
		
		consentTierResponse.add(consentTierRespObj);
		consentTierRespObj.setStatement("Consent to use blood");
		consentDetail.setConsentTierResponses(consentTierResponse);
		cprDetail.setConsentDetails(consentDetail);
		cprDetail.setRegistrationDate(asigPart.getDateOfStatusChange());//setCpTitle
		cprDetail.setCpTitle(CP_TITLE);
	}
	
	private String getVitalStatus(int code) {
		switch(code) {
			case 0:
				return "Alive";
		 	case 1:
		 		return "Dead";
		 	case 2:
		 		return "Unknown";
		 	case 3: 
		 		return "Unspecified";
		 	default:
		 		return "";
		 }
	}

	private <T> RequestEvent<T> getRequest(T payload) {
		return new RequestEvent<T>(payload);
	}
        
    private static final String INSTITUTE_NAME = "MCRI";
    
    private static final String CP_TITLE = "ASIG";
    
    private static final String PHONE_NUMBER = "+61 404622745";
    
    private static final String ADMIN_EMAIL = "abhijeet.thakurdesai@krishagni.com";
    
    private static final String TISSUE_BANKER_ROLE = "Tissue Banker";
    
    private static final String SITE_TYPE_REPOSITORY = "Repository";
	
}