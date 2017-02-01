
package com.krishagni.openspecimen.epic.service.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.ParticipantMedicalIdentifier;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.biospecimen.services.ParticipantService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.EmailService;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.openspecimen.epic.dao.EpicDao;
import com.krishagni.openspecimen.epic.events.CprDetail;
import com.krishagni.openspecimen.epic.events.EpicMergeFailedDTO;
import com.krishagni.openspecimen.epic.events.EpicParticipantDetail;
import com.krishagni.openspecimen.epic.events.EpicPmiDetail;
import com.krishagni.openspecimen.epic.events.ParticipantResponse;
import com.krishagni.openspecimen.epic.service.EpicService;
import com.krishagni.openspecimen.epic.util.ResponseStatus;

public class EpicServiceImpl implements EpicService {
	
	private static final Log logger = LogFactory.getLog(EpicServiceImpl.class);

	private int added_count;

	private int edited_count;

	private int failed_count;
	
	private Boolean isMrnError = false;
	
	private Boolean isDisabled = false;
	
	private Long pId = 0L;
	
	private Connection connection;

	private EpicDao epicDao;

	private DaoFactory daoFactory;

	private ParticipantService participantSvc;
	
	private CollectionProtocolRegistrationService cprSvc;

	private EmailService emailSvc;

	public void setEpicDao(EpicDao epicDao) {
		this.epicDao = epicDao;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setParticipantSvc(ParticipantService participantSvc) {
		this.participantSvc = participantSvc;
	}
	
	public void setCprSvc(CollectionProtocolRegistrationService cprSvc) {
		this.cprSvc = cprSvc;
	}

	public void setEmailSvc(EmailService emailSvc) {
		this.emailSvc = emailSvc;
	}

	@Override
	public void registerParticipants() {
		added_count = 0;
		failed_count = 0;
		edited_count = 0;
		
		try {
			connection = initConnection();
			logger.info("Epic job started............");
			List<EpicParticipantDetail> epicPartList = epicDao.getEpicParticipantDetails(connection); 
			updateSecurityContext();
			List<EpicMergeFailedDTO> failedMergeRecordList = new ArrayList<EpicMergeFailedDTO>();
			for (EpicParticipantDetail epicPart : epicPartList) {
				logger.debug("Updating the participant with PartSourceId: " + epicPart.getId() + " PartSource: " + epicPart.getSource());
				String successMsg = "";
				String errorMsg = "";
				String customMsg = "";
				ParticipantDetail detail = new ParticipantDetail();
				try{
					populateParticipantDetail(detail, epicPart, false);
					logger.debug("Started processing the participant with PartSourceId: " + epicPart.getId() + " PartSource: " + epicPart.getSource());
					successMsg = createEpicPatient(failedMergeRecordList, epicPart, detail);
					logger.debug("EPIC after create participant : " + successMsg);
					if(successMsg.contains(":custom")) {
						String[] msgs = successMsg.split(":custom");
						successMsg = msgs[0];
						customMsg = msgs[1];
					}
				} catch(OpenSpecimenException e) {
					detail.setPmis(EpicPmiDetail.to(
						epicDao.getPmis(connection, epicPart.getId(), epicPart.getSource(), true), false));
					successMsg = MESSAGE_PARTICIPANT_FAILED;
					errorMsg = e.getMessage();
					logger.error("Error processing participant with PartSourceId: " + epicPart.getId() + " PartSource: " + epicPart.getSource() + ". Error : " + errorMsg);
					if(CollectionUtils.isNotEmpty(detail.getPmis()) && isMrnError){
						errorMsg = "Participant with same MRN already exists";
						isMrnError = false;
					}
					
					if(isDisabled) {
						errorMsg = "Participant with same MRN already exists, Participant is Disabled/Closed.";
						isDisabled = false;
						epicPart.setOsId(pId);
					}

					String partSrcId = epicPart.getId();
					String catPartSrcId = String.valueOf(epicPart.getOsId());
					String source = epicPart.getSource();

					List<com.krishagni.catissueplus.core.biospecimen.events.PmiDetail> pmiDetails = detail.getPmis();
					String mrn = CollectionUtils.isNotEmpty(pmiDetails) ? pmiDetails.get(0).getMrn() : "";
					String siteName = CollectionUtils.isNotEmpty(pmiDetails) ? pmiDetails.get(0).getSiteName() : "";
					customMsg = " Custom message- MRN: " + mrn + ", Site Name: " + siteName + ", Part Source Id: " + partSrcId +
							", OpenSpecimen Part Id: " + catPartSrcId + ", Part Source: " + source;
					failed_count++;
				}
				updateAuditLog(epicPart, successMsg, errorMsg, customMsg);
			}
		}
		catch (SQLException e) {
			logger.error("Error during connection initialisation", e);
		} finally {
			Map<String, Object> emailProps = new HashMap<String, Object>();
			emailProps.put("addedCount", added_count);
			emailProps.put("editedCount", edited_count);
			emailProps.put("failedCount", failed_count);
			emailProps.put("jobDate", new Date());
			emailSvc.sendEmail("epic_participant_job_template",
					new String[] { ConfigUtil.getInstance().getAdminEmailId() }, emailProps);
			try {
				if (connection != null) {
					epicDao.updateCatissueStagingAudit(connection, added_count, edited_count, failed_count);
					connection.commit();
					connection.close();
				}
			} catch (SQLException e) {
				logger.error("Error while closing the connection", e);
			}
		}
	}

	@PlusTransactional
	private void updateSecurityContext() {
		String adminEmailId = ConfigUtil.getInstance().getStrSetting(EPIC_MODULE, "admin_email_id", "admin@admin.com");
		String adminDomain = ConfigUtil.getInstance().getStrSetting(EPIC_MODULE, "admin_auth_domain", "openspecimen");
		User user = daoFactory.getUserDao().getUser(adminEmailId, adminDomain);
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user,null, user.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(token);
	}

	private void updateAuditLog(EpicParticipantDetail epicPart, String successMsg, String errorMsg, String customMsg) throws SQLException {
		epicDao.updateAuditLog(connection, epicPart.getSource(), epicPart.getId(), successMsg, errorMsg, customMsg);
	}

	@PlusTransactional
	private String createEpicPatient(List<EpicMergeFailedDTO> failedMergeRecordList, EpicParticipantDetail epicPart, ParticipantDetail detail) throws SQLException {
		String successMsg;
		boolean isMergable = isMergable(epicPart, epicPart.getId(), epicPart.getChangeType());
		logger.debug("Is Mergable : " + isMergable);
		if (isMergable) {
			mergeParticipant(epicPart, detail, failedMergeRecordList);
		} 
		successMsg = updateParticipant(epicPart, detail);
		logger.debug("EPIC after update participant : " + successMsg);
		return successMsg;
	}

	private void mergeParticipant(EpicParticipantDetail epicPart, ParticipantDetail detail, List<EpicMergeFailedDTO> failedMergeRecordList) throws SQLException {
		ParticipantDetail oldParticipantDetail = new ParticipantDetail();
		populateParticipantDetail(oldParticipantDetail, epicPart, true); //Use boolean flag to use old or new
		if(oldParticipantDetail.getId() == null){
			oldParticipantDetail.setId(getPatientIdByMrn(epicPart));
		}
		
		detail.setId(oldParticipantDetail.getId());
	}

	private CollectionProtocolRegistrationDetail getCprDetail(ParticipantDetail detail, CprDetail cprDetail) {
		CollectionProtocolRegistrationDetail cpr = new CollectionProtocolRegistrationDetail();
		cpr.setParticipant(detail);
		cpr.setCpId(epicDao.getCpIdByIrbId(cprDetail.getIrbID(), cprDetail.getShortTitle()));//Also get the CPR Id by partId and CPID
		cpr.setRegistrationDate(cprDetail.getRegistrationDate());
		return cpr;
	}
	
	private String updateParticipant(EpicParticipantDetail epicPart, ParticipantDetail detail) throws SQLException {
		String successMessage = "";
		ParticipantResponse response = new ParticipantResponse();
		logger.debug("Updating participant with Id: " + detail.getId());
		List<CprDetail> cprList = epicDao.getCprDetails(connection, epicPart.getId(), epicPart.getSource(), true);
		
		
		if (detail.getId() == null) {
			logger.debug("isAdd, getting ID by MRN");
			Long participantId = epicDao.getParticipantIdFromMrn(connection, epicPart);
			detail.setId(participantId);
		}

		boolean isAdd = detail.getId() == null ? true : false;

		if (!isAdd) {
			Participant participant = epicDao.getParticipant(detail.getId());
			if (participant == null || !participant.isActive()) {
				logger.debug("MRN already exist, Participant is Disabled/Closed");
				isDisabled = true;
				pId = detail.getId();
				throw new OpenSpecimenException(ErrorType.USER_ERROR);
			}
		}
		
		boolean isToUpdateMrn = true;
		for (CprDetail cprDetail : cprList) {
			isAdd = detail.getId() == null ? true : false;
			if(!isAdd) {
				Participant participant = epicDao.getParticipant(detail.getId());
				if(participant == null || !participant.isActive()) {
					logger.debug("MRN already exist, Participant is Disabled/Closed");
					isDisabled = true;
					pId = detail.getId();
					throw new OpenSpecimenException(ErrorType.USER_ERROR);
				}
			}
			
			logger.debug("EPIC is add : " + isAdd);
			logger.debug("Participant Id : " + detail.getId());
			
			if(isAdd) {
				isToUpdateMrn = false;
				if(isMrnAlreadyExists(detail)){
					logger.debug("MRN already exists for participant with Id: " + epicPart.getId() + " Source : " + epicPart.getSource());
					isMrnError = true;
					throw new OpenSpecimenException(ErrorType.USER_ERROR);
				}
				
				detail.setPmis(EpicPmiDetail.to(epicDao.getPmis(connection, epicPart.getId(), epicPart.getSource(), false), false));
			}
			
			logger.debug("EPIC updating registration");
			CollectionProtocolRegistrationDetail cpr = getCprDetail(detail, cprDetail);
			Long cprId = null;
			if(!isAdd) {
				Long participantId = detail.getId();
				Long cpId = cpr.getCpId();
				cprId = epicDao.getCprId(participantId, cpId);
			}
			if(cprId != null) {
				logger.debug("Participant already registered, hence skipping the registration.");
			}

			if(cprId == null) {
				if(isAdd) {
					logger.debug("Creating participant............");
					ResponseEvent<ParticipantDetail> result = participantSvc.createParticipant(getRequest(cpr.getParticipant()));
					result.throwErrorIfUnsuccessful();
					detail = result.getPayload();
					logger.debug("Participant created successfully.....");
					cpr.setParticipant(detail);
				}
				
				String ppid = cpr.getCpId() + "_" + cpr.getParticipant().getId();
				cpr.setPpid(ppid);
				ResponseEvent<CollectionProtocolRegistrationDetail> result = cprSvc.createRegistration(getRequest(cpr));
				result.throwErrorIfUnsuccessful();
				detail = result.getPayload().getParticipant();

				updateConsentDate(result.getPayload().getId(), cprDetail.getConsentSignatureDate());
			}
		}
		
		if(epicPart.isUpdatable()){
			logger.debug("EPIC Updating participant with id : " + detail.getId());
			detail = updateParticipant(detail);
		}
		logger.debug("EPIC Updating MRN");
		if(isToUpdateMrn) { 
			detail = updateMrn(epicPart, detail);
		}
		
		logger.debug("EPIC Updating Consents");
		updateConsents(epicPart, detail);

		response.setParticipantDetail(detail);
		response.setResponseStatus(isToUpdateMrn ? ResponseStatus.MODIFIED : ResponseStatus.ADDED);
		
		//Update the participant by calling the update REST API, the details will be in the detail object.

		logger.debug("EPIC updating mapping table");
		successMessage = updateMappingTable(response, epicPart.getId(), epicPart.getSource());
		logger.debug(" EPIC after mapping table : " + successMessage);
		List<com.krishagni.catissueplus.core.biospecimen.events.PmiDetail> pmiDetails = detail.getPmis();
		String mrn = CollectionUtils.isNotEmpty(pmiDetails) ? pmiDetails.get(0).getMrn() : "";
		String siteName = CollectionUtils.isNotEmpty(pmiDetails) ? pmiDetails.get(0).getSiteName() : "";
		
		String customMsg = "";
		customMsg = " Custom message- MRN: " + mrn + ", Site Name: " + siteName + ", Part Source Id: " + epicPart.getId() +
				", OpenSpecimen Part Id: " + detail.getId() + ", Part Source: " + epicPart.getSource();
		logger.debug("Epic building custom message : " + customMsg);
		return successMessage + ":custom" + customMsg;

	}

	private void updateConsentDate(Long cprId, Date consentDate) {
		ConsentDetail detail = new ConsentDetail();
		detail.setCprId(cprId);
		detail.setConsentSignatureDate(consentDate);

		cprSvc.saveConsents(getRequest(detail));
	}

	private void updateConsents(EpicParticipantDetail epicPart, ParticipantDetail detail) throws SQLException {
		List<ConsentDetail> consents = epicDao.getConsents(connection, epicPart.getId(), epicPart.getSource(), detail.getId());
		
		for (ConsentDetail consentDetail : consents) {
			List<CprDetail> list = epicDao.getCprDetails(connection, epicPart.getId(), epicPart.getSource(), false);
			if (CollectionUtils.isNotEmpty(list)) {
				consentDetail.setConsentSignatureDate(list.stream().findFirst().get().getConsentSignatureDate());
			}

			ResponseEvent<ConsentDetail> consentResult = cprSvc.saveConsents(getRequest(consentDetail));
			consentResult.throwErrorIfUnsuccessful();
		}
	}
	
	private ParticipantDetail updateParticipant(ParticipantDetail detail) {
		logger.debug("In the participant update method, participant id is : " + detail.getId());
		ResponseEvent<ParticipantDetail> result = participantSvc.patchParticipant(getRequest(detail));
		result.throwErrorIfUnsuccessful();
		detail = result.getPayload();
		return detail;
	}

	private ParticipantDetail updateMrn(EpicParticipantDetail epicPart, ParticipantDetail detail) throws SQLException {
		ParticipantDetail pDetail = new ParticipantDetail();
		pDetail.setId(detail.getId());

		List<PmiDetail> pmisToUpdate = new ArrayList<PmiDetail>();
		List<EpicPmiDetail> newResult = epicDao.getPmis(connection, epicPart.getId(), epicPart.getSource(), true);
		
		Participant part = daoFactory.getParticipantDao().getById(detail.getId());
		Set<ParticipantMedicalIdentifier> pmiSet = part.getPmis();
		List<PmiDetail> origPmiList = PmiDetail.from(pmiSet, false);
		
		pmisToUpdate.addAll(EpicPmiDetail.to(newResult, true));
		
		for (PmiDetail pmiDetail : origPmiList) {
			boolean mrnFlag = true;
			for (EpicPmiDetail pmi : newResult) {
				if(pmi.getMrnValue().equals(pmiDetail.getMrn())) {
					mrnFlag = false;
				}
			}
			if(mrnFlag) {
				pmisToUpdate.add(pmiDetail);
			}
		}
		
		pDetail.setPmis(pmisToUpdate);
		if(isMrnAlreadyExists(pDetail)){
			logger.debug("MRN already exists for participant with Id: " + epicPart.getId() + " Source : " + epicPart.getSource());
			isMrnError = true;
			throw new OpenSpecimenException(ErrorType.USER_ERROR);
		}
		
		if(pmisToUpdate != null && !pmisToUpdate.isEmpty()){
			ResponseEvent<ParticipantDetail> result = participantSvc.patchParticipant(getRequest(pDetail));
			result.throwErrorIfUnsuccessful();
			detail = result.getPayload();
		}
		return detail;
	}

	private boolean isMrnAlreadyExists(ParticipantDetail detail) {
		if (detail.getPmis() != null && !detail.getPmis().isEmpty()) {
			List<Long> participantIds = daoFactory.getParticipantDao().getParticipantIdsByPmis(
					detail.getPmis());
			if (participantIds != null && !participantIds.isEmpty()) {
				if(detail.getId() == null){
					return true;
				} else {
					return !participantIds.contains(detail.getId());
				}
			}
		}
		return false;
	}

	private void populateParticipantDetail(ParticipantDetail partDetail, EpicParticipantDetail epicPart,
			boolean isOldObj) throws SQLException {
		Set<String> races = epicDao.getRace(connection, isOldObj ? epicPart.getOldId() : epicPart.getId(), epicPart.getSource());
		if(CollectionUtils.isNotEmpty(races)){
			partDetail.setRaces(races);
		}
		partDetail.setFirstName(epicPart.getFirstName());
		partDetail.setMiddleName(epicPart.getMiddleName());
		partDetail.setLastName(epicPart.getLastName());
		partDetail.setBirthDate(epicPart.getBirthDate());
		partDetail.setGender(epicPart.getGender());
		partDetail.setVitalStatus(epicPart.getVitalStatus());
		partDetail.setDeathDate(epicPart.getDeathDate());
		partDetail.setEthnicity(epicPart.getEthnicity());
		
		if(isOldObj) {
			partDetail.setId(epicDao.getOsParticipantId(connection, epicPart.getOldId(),
					epicPart.getSource()));
		} else {
			Long existingId = getPatientIdByMrn(epicPart);
			if(existingId == null) {
				existingId = epicDao.getOsParticipantId(connection, epicPart.getId(), epicPart.getSource());
			}
			partDetail.setId(existingId == null ? partDetail.getId() : existingId);
		}
		
	}

	@PlusTransactional
	private Long getPatientIdByMrn(EpicParticipantDetail epicPart) throws SQLException {
		List<EpicPmiDetail> pmis = epicDao.getPmis(connection, epicPart.getId(), epicPart.getSource(), false);
		
		if (pmis != null && !pmis.isEmpty()) {
			List<Long> participantIds = daoFactory.getParticipantDao().getParticipantIdsByPmis(
					EpicPmiDetail.to(pmis, false));
			if (CollectionUtils.isNotEmpty(participantIds)) {
				return participantIds.get(0);
			} else {
				List<Long> pIds = daoFactory.getParticipantDao().getParticipantIdsByPmis(
						EpicPmiDetail.to(pmis, true));
				if (CollectionUtils.isNotEmpty(pIds)) {
					return pIds.get(0);
				}
			}
		}
		
		return null;
	}

	@PlusTransactional
	private void populateParticipantId(ParticipantDetail participantDetailsDTO) {
		if (participantDetailsDTO.getId() != null) {
			return;
		}
		if (participantDetailsDTO.getPmis() != null && !participantDetailsDTO.getPmis().isEmpty()) {
			List<Long> participantIds = daoFactory.getParticipantDao().getParticipantIdsByPmis(
					participantDetailsDTO.getPmis());
			if (participantIds != null && !participantIds.isEmpty()) {
				participantDetailsDTO.setId(participantIds.get(0));
			}
		}
	}
	
	private Connection initConnection() throws SQLException {
		String dbServer = ConfigUtil.getInstance().getStrSetting(EPIC_MODULE, "database_server", "");
		String dbPort = ConfigUtil.getInstance().getStrSetting(EPIC_MODULE, "database_port", "");
		String dbName = ConfigUtil.getInstance().getStrSetting(EPIC_MODULE, "database_name", "");
		String dbUser = ConfigUtil.getInstance().getStrSetting(EPIC_MODULE, "database_user", "");
		String dbPassword = ConfigUtil.getInstance().getStrSetting(EPIC_MODULE, "database_password", "");

		String url = "jdbc:oracle:thin:@" + dbServer + ":" + dbPort + ":" + dbName;
		// String url = "jdbc:mysql://"+dbServer+":"+dbPort+"/"+dbName;
		Connection conn = null;
		conn = java.sql.DriverManager.getConnection(url, dbUser, dbPassword);
		return conn;
	}

	private String updateMappingTable(ParticipantResponse response, String staginParticipantId,
			String staginPartIdSource) throws SQLException {
		String successMessage = "";
		String mrn = "";
		if(CollectionUtils.isNotEmpty(response.getParticipantDetail().getPmis())){
			mrn = response.getParticipantDetail().getPmis().iterator().next().getMrn();
		}
		switch (response.getStatus()) {
			case ADDED :
				epicDao.insertMapping(connection, response.getParticipantDetail().getId(), staginParticipantId, staginPartIdSource, mrn);
				successMessage = MESSAGE_PARTICIPANT_ADDED;
				added_count++;
				break;
			case MODIFIED :
				epicDao.updateMapping(connection, response.getParticipantDetail().getId(), staginParticipantId, staginPartIdSource, mrn);
				successMessage = MESSAGE_PARTICIPANT_UPDATED;
				edited_count++;
				break;
			case MRNDELETED :
				epicDao.updateMapping(connection, response.getParticipantDetail().getId(), staginParticipantId, staginPartIdSource, mrn);
				successMessage = MESSAGE_MRN_DELETED;
				edited_count++;
				break;
			case ERROR :
				successMessage = MESSAGE_SCENARIO_NOT_HANDLED;
				edited_count++;
				break;
			case MERGEFAILED :
				successMessage = MESSAGE_SCENARIO_NOT_HANDLED;
				edited_count++;
				break;
		}
		return successMessage;
	}

	private boolean isMergable(EpicParticipantDetail epicPart, String id, String changeType) {
		return (CHANGE_TYPE_MERGE.equals(changeType) || CHANGE_TYPE_CHANGE.equals(changeType) || CHANGE_TYPE_UNMERGE
				.equals(changeType)) && !id.equals(epicPart.getOldId());
	}
	
	private <T> RequestEvent<T> getRequest(T payload) {
		return new RequestEvent<T>(payload);
	}

	private final String CHANGE_TYPE_MERGE = "MERGE";

	private final String CHANGE_TYPE_UNMERGE = "UNMERGE";

	private final String CHANGE_TYPE_CHANGE = "CHANGE";

	private final String MESSAGE_PARTICIPANT_ADDED = "Participant Added";

	private final String MESSAGE_PARTICIPANT_UPDATED = "Participant UPDATED";

	private final String MESSAGE_MRN_DELETED = "Participant MRN DELETED";

	private final String MESSAGE_PARTICIPANT_FAILED = "Failed";
	
	private final String MESSAGE_SCENARIO_NOT_HANDLED = "Scenario not handled";
	
	private static final String EPIC_MODULE = "plugin_epic";

}
