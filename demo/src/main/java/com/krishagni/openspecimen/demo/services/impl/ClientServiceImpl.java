package com.krishagni.openspecimen.demo.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.EmailService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.openspecimen.demo.events.ClientDetail;
import com.krishagni.openspecimen.demo.services.ClientService;

public class ClientServiceImpl implements ClientService {
	private static final String SIGN_UP_EMAIL_TMPL = "new_user_request";
	
	private static final String SIGNED_UP_EMAIL_TMPL = "users_signed_up";
  
	private DaoFactory daoFactory;
	
	private EmailService emailService;
	
	private UserFactory userFactory;
  
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setEmailService(EmailService emailService) {
		this.emailService = emailService;
	}
  
	public void setUserFactory(UserFactory userFactory) {
		this.userFactory = userFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ClientDetail> createUser(RequestEvent<ClientDetail> req) {
		try {
			boolean isSignupReq = (AuthUtil.getCurrentUser() == null);
	  
			ClientDetail detail = req.getPayload();
			if (isSignupReq) {
				detail.setActivityStatus(Status.ACTIVITY_STATUS_PENDING.getStatus());
			}

			User user = userFactory.createUser(detail);
	  
			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureUniqueLoginNameInDomain(user.getLoginName(), user.getAuthDomain().getName(), ose);
			ensureUniqueEmailAddress(user.getEmailAddress(), ose);
			ose.checkAndThrow();
		
			daoFactory.getUserDao().saveOrUpdate(user);

			detail.setId(user.getId());
			
			processClientDetail(detail);
			sendUserSignupEmail(detail);
			sendNewUserRequestEmail(detail);
	  
			return ResponseEvent.response(detail);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private void processClientDetail(ClientDetail detail) {
		List<String> specimenCollectionList = new ArrayList<String>();
		List<String> osIntegrationList = new ArrayList<String>();
		
		if(detail.getPlannedCollection() != null && detail.getPlannedCollection()) {
			specimenCollectionList.add("Planned clinical study based collections");
		}
		if(detail.getUnplannedCollection() != null && detail.getUnplannedCollection()) {
			specimenCollectionList.add("General biobanking unplanned collections");
		}
		
		if(detail.getCdms() != null && detail.getCdms()) {
			osIntegrationList.add("Clinical Data Management System");
		}
		if(detail.getBarcodePrinters() != null && detail.getBarcodePrinters()) {
			osIntegrationList.add("Barcode Printers");
		}
		if(detail.getCerner() != null && detail.getCerner()) {
			osIntegrationList.add("Cerner (or other path database)");
		}
		if(detail.getRedcap() != null && detail.getRedcap()) {
			osIntegrationList.add("RedCAP");
		}
		if(detail.getOpenClinica() != null && detail.getOpenClinica()) {
			osIntegrationList.add("OpenClinica");
		}
		if(detail.getVelos() != null && detail.getVelos()) {
			osIntegrationList.add("EPIC/Velos");
		}
		if(detail.getOsIntegration() != null && detail.getOsIntegration()) {
			String str = "Others";
			if(StringUtils.isNotBlank(detail.getOsIntegrationOthers())) {
				str += ": " + detail.getOsIntegrationOthers();
			}
			osIntegrationList.add(str);
		}
		
		detail.setSpecimenCollection(StringUtils.join(specimenCollectionList, ", "));
		detail.setOsIntegrationValue(StringUtils.join(osIntegrationList, ", "));
	}

	private void ensureUniqueLoginNameInDomain(String loginName, String domainName, OpenSpecimenException ose) {
		if(User.SYS_USER.equals(loginName.trim()) && User.DEFAULT_AUTH_DOMAIN.equals(domainName.trim())) {
			ose.addError(UserErrorCode.SYS_LOGIN_NAME, loginName);
		}
	
		if(!daoFactory.getUserDao().isUniqueLoginName(loginName, domainName)) {
			ose.addError(UserErrorCode.DUP_LOGIN_NAME);
		}
	}

	private void ensureUniqueEmailAddress(String emailAddress, OpenSpecimenException ose) {
		if(!daoFactory.getUserDao().isUniqueEmailAddress(emailAddress)) {
			ose.addError(UserErrorCode.DUP_EMAIL);
		}
	}
  
	private void sendNewUserRequestEmail(ClientDetail detail) {
		String[] to = {ConfigUtil.getInstance().getAdminEmailId()};
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("user", detail);
	
		emailService.sendEmail(SIGN_UP_EMAIL_TMPL, to, props);
	}
	
	private void sendUserSignupEmail(ClientDetail detail) {
	  String[] to = new String[]{detail.getEmailAddress()};
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("user", detail);
		
		emailService.sendEmail(SIGNED_UP_EMAIL_TMPL, to, props);
	}
}
