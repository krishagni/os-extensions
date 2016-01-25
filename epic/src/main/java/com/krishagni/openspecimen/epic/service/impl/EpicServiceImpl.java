
package com.krishagni.openspecimen.epic.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
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
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.openspecimen.epic.dao.EpicDao;
import com.krishagni.openspecimen.epic.events.CprDetail;
import com.krishagni.openspecimen.epic.events.EpicMergeFailedDTO;
import com.krishagni.openspecimen.epic.events.EpicParticipantDetail;
import com.krishagni.openspecimen.epic.events.ParticipantResponse;
import com.krishagni.openspecimen.epic.events.PmiDetail;
import com.krishagni.openspecimen.epic.service.EpicService;
import com.krishagni.openspecimen.epic.util.ResponseStatus;

public class EpicServiceImpl implements EpicService {

	private int added_count;

	private int edited_count;

	private int failed_count;

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
			List<EpicParticipantDetail> epicPartList = getEpicParticipantDetails();
			
			List<EpicMergeFailedDTO> failedMergeRecordList = new ArrayList<EpicMergeFailedDTO>();
			for (EpicParticipantDetail epicPart : epicPartList) {
				String successMsg = "";
				String errorMsg = "";
				try{
					successMsg = createEpicPatient(failedMergeRecordList, epicPart);
				} catch(OpenSpecimenException e) {
					successMsg = MESSAGE_PARTICIPANT_FAILED;
					errorMsg = e.getMessage();
					if(errorMsg.contains("MRN already exists")){
						String partSrcId = epicPart.getId();
						String catPartSrcId = String.valueOf(epicPart.getOsId());
						String source = epicPart.getSource();
						List<PmiDetail> pmiDetails = epicPart.getPmiDetails();
						String mrn = CollectionUtils.isNotEmpty(pmiDetails) ? pmiDetails.get(0).getMrnValue() : "";
						String siteName = pmiDetails != null ? pmiDetails.get(0).getSiteName() : "";
						String mrnMsg = " Custom message- MRN: " + mrn + ", Site Name: " + siteName + ", Part Source Id: " + partSrcId +
								", OpenSpecimen Part Id: " + catPartSrcId + ", Part Source: " + source;
						errorMsg = errorMsg + mrnMsg;
					}
					failed_count++;
				}
				updateAuditLog(epicPart, successMsg, errorMsg);
			}
		} finally {
			Map<String, Object> emailProps = new HashMap<String, Object>();
			emailProps.put("addedCount", added_count);
			emailProps.put("editedCount", edited_count);
			emailProps.put("failedCount", failed_count);
			emailProps.put("jobDate", new Date());
			emailSvc.sendEmail("epic_participant_job_template", new String[] {ConfigUtil.getInstance().getAdminEmailId()}, emailProps);
		}
	}

	@PlusTransactional
	private void updateAuditLog(EpicParticipantDetail epicPart, String successMsg, String errorMsg) {
		epicDao.updateAuditLog(epicPart.getSource(), epicPart.getId(), successMsg, errorMsg);
	}

	@PlusTransactional
	private String createEpicPatient(List<EpicMergeFailedDTO> failedMergeRecordList, EpicParticipantDetail epicPart) {
		String successMsg;
		boolean isMergable = isMergable(epicPart, epicPart.getId(), epicPart.getChangeType()); 
		if (isMergable) {
			successMsg = mergeParticipant(epicPart, failedMergeRecordList);
		} else {
			successMsg = updateParticipant(epicPart);
		}
		return successMsg;
	}

	@PlusTransactional
	private List<EpicParticipantDetail> getEpicParticipantDetails() {
		List<EpicParticipantDetail> epicPartList = epicDao.getEpicParticipantDetails();
		User user = daoFactory.getUserDao().getUser("admin@admin.com", "openspecimen");
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user,null, user.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(token);
		return epicPartList;
	}

	private String mergeParticipant(EpicParticipantDetail epicPart, List<EpicMergeFailedDTO> failedMergeRecordList) {
		ParticipantDetail detail = new ParticipantDetail();
		
		populateParticipantDetail(detail, epicPart, false);

		ParticipantDetail oldParticipantDetail = new ParticipantDetail();
		populateParticipantDetail(oldParticipantDetail, epicPart, true); //Use boolean flag to use old or new
		List<CprDetail> cprList = epicDao.getCprDetails(epicPart.getId(), epicPart.getSource());
		String successMessage = "";

		boolean oldPatientHasSpecimens = epicDao.hasSpecimens(oldParticipantDetail.getId()); //Call participant.delete() API 
		boolean patientHasSpecimens = epicDao.hasSpecimens(detail.getId()); //participantBizLogic.hasSpecimen(participantDetailsDTO.getId())
		if ((oldParticipantDetail.getId() != null && oldPatientHasSpecimens)
				|| (detail.getId() != null && patientHasSpecimens)) {

			EpicMergeFailedDTO epicMergeFailedDTO = new EpicMergeFailedDTO();
			epicMergeFailedDTO.setOldPartSourceID(epicPart.getOldId());
			epicMergeFailedDTO.setPartSourceID(epicPart.getId());
			epicMergeFailedDTO.setOldParticipantDetailsDTO(oldParticipantDetail);
			epicMergeFailedDTO.setParticipantDetailsDTO(detail);
			epicMergeFailedDTO.setChangeType(epicPart.getChangeType());
			epicMergeFailedDTO.setPartSource(epicPart.getSource());
			failedMergeRecordList.add(epicMergeFailedDTO);
			
			successMessage = "Failed because participant has collected specimen";
			failed_count++;
		} else if(cprList == null || cprList.isEmpty()){
			successMessage = "Failed! Registraiton flag not set for this participant";
			failed_count++;
		} else {
		//TODO check if CPR details not present then skip the registration
			
			ParticipantResponse response = new ParticipantResponse();
			if(detail.getId() != null){
				//response //TODO: call REST API to update participant with details from detail Object 
				ResponseEvent<ParticipantDetail> result = participantSvc.updateParticipant(getRequest(detail));
				result.throwErrorIfUnsuccessful();
				
				if(!cprList.isEmpty()){
					for (CprDetail cprDetail : cprList) {
						CollectionProtocolRegistrationDetail cpr = getCprDetail(detail, cprDetail);
						saveConsents(cprDetail, epicDao.getCprId(detail.getId(), cprDetail.getCpId()));
					}
				}
	
				response.setParticipantDetail(result.getPayload());
				response.setResponseStatus(ResponseStatus.MODIFIED);
			} else {
				for (CprDetail cprDetail : cprList) {
					CollectionProtocolRegistrationDetail cpr = getCprDetail(detail, cprDetail);
					ResponseEvent<CollectionProtocolRegistrationDetail> result = cprSvc.createRegistration(getRequest(cpr));
					
					result.throwErrorIfUnsuccessful();
					
					saveConsents(cprDetail, result.getPayload().getId());
					
					response.setParticipantDetail(result.getPayload().getParticipant());
					response.setResponseStatus(ResponseStatus.ADDED);
					
					}
				}
			if ((CHANGE_TYPE_MERGE.equals(epicPart.getChangeType()) || CHANGE_TYPE_CHANGE.equals(epicPart.getChangeType()))) {
				oldParticipantDetail.setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.toString());
			}

			//call REST API to update participant with details from oldParticipantDetail object
			ResponseEvent<ParticipantDetail> updateRes = participantSvc.updateParticipant(getRequest(oldParticipantDetail));
			
			updateRes.throwErrorIfUnsuccessful();

			successMessage = updateMappingTable(response, epicPart.getId(), epicPart.getSource());
			}
		return successMessage;
	}

	private CollectionProtocolRegistrationDetail getCprDetail(ParticipantDetail detail, CprDetail cprDetail) {
		CollectionProtocolRegistrationDetail cpr = new CollectionProtocolRegistrationDetail();
		cpr.setParticipant(detail);
		cpr.setCpId(epicDao.getCpIdByIrbId(cprDetail.getIrbID()));
		cpr.setRegistrationDate(cprDetail.getRegistrationDate());
		return cpr;
	}
	
	private String updateParticipant(EpicParticipantDetail epicPart) {
		ParticipantDetail detail = new ParticipantDetail();
		String successMessage = "";
		populateParticipantDetail(detail, epicPart, false);

		ParticipantResponse response = new ParticipantResponse();
		
		List<CprDetail> cprList = epicDao.getCprDetails(epicPart.getId(), epicPart.getSource());

		if (detail.getId() == null) {
			if(cprList.isEmpty()){
				throw new OpenSpecimenException(ErrorType.USER_ERROR);
			}
			for (CprDetail cprDetail : cprList) {
				CollectionProtocolRegistrationDetail cpr = getCprDetail(detail, cprDetail);
				ResponseEvent<CollectionProtocolRegistrationDetail> result = cprSvc.createRegistration(getRequest(cpr));
				result.throwErrorIfUnsuccessful();
				
				saveConsents(cprDetail, result.getPayload().getId());
				
				response.setParticipantDetail(result.getPayload().getParticipant());
				response.setResponseStatus(ResponseStatus.ADDED);
			}

			//Create participant by calling the create REST API, the details will be in the detail object.
		} else {
			ResponseEvent<ParticipantDetail> result = participantSvc.updateParticipant(getRequest(detail));
			result.throwErrorIfUnsuccessful();
			
			if(!cprList.isEmpty()){
				for (CprDetail cprDetail : cprList) {
					CollectionProtocolRegistrationDetail cpr = getCprDetail(detail, cprDetail);
					saveConsents(cprDetail, epicDao.getCprId(detail.getId(), cpr.getCpId()));
				}
			}

			response.setParticipantDetail(result.getPayload());
			response.setResponseStatus(ResponseStatus.MODIFIED);
			//Update the participant by calling the update REST API, the details will be in the detail object.
		}

		successMessage = updateMappingTable(response, epicPart.getId(), epicPart.getSource());
		return successMessage;

	}

	private void saveConsents(CprDetail cprDetail, Long cprId) {
		ConsentDetail consentDetail = new ConsentDetail();
		consentDetail.setConsentSignatureDate(cprDetail.getConsentSignatureDate());
		consentDetail.setCprId(cprId);
		consentDetail.setConsentTierResponses(cprDetail.getConsentResponseList());
			
		ResponseEvent<ConsentDetail> consentResult = cprSvc.saveConsents(getRequest(consentDetail));
		consentResult.throwErrorIfUnsuccessful();
	}

	private void populateParticipantDetail(ParticipantDetail partDetail, EpicParticipantDetail epicPart,
			boolean isOldObj) {
		//		participantDetailsDTO.setCollectionProtocolRegistrationDTOList(
		epicDao.getCprDetails(epicPart.getId(), epicPart.getSource());
		partDetail.setRaces(
				epicDao.getRace(isOldObj ? epicPart.getOldId() : epicPart.getId(), epicPart.getSource()));
		List<com.krishagni.catissueplus.core.biospecimen.events.PmiDetail> pmi = 
				epicDao.getPmis(isOldObj ? epicPart.getOldId() : epicPart.getId(), epicPart.getSource());
		partDetail.setPmis(pmi);
		epicPart.setPmiDetails(PmiDetail.from(pmi));
		partDetail.setFirstName(epicPart.getFirstName());
		partDetail.setMiddleName(epicPart.getMiddleName());
		partDetail.setLastName(epicPart.getLastName());
		partDetail.setBirthDate(epicPart.getBirthDate());
		partDetail.setGender(epicPart.getGender());
		partDetail.setVitalStatus(epicPart.getVitalStatus());
		partDetail.setDeathDate(epicPart.getDeathDate());
		partDetail.setEthnicity(epicPart.getEthnicity());
		populateParticipantId(partDetail);
		
		if(isOldObj) {
			partDetail.setId(epicDao.getOsParticipantId(epicPart.getOldId(),
					epicPart.getSource()));
		} else {
			Long existingId = epicDao.getOsParticipantId(epicPart.getId(), epicPart.getSource());
			partDetail.setId(existingId == null ? partDetail.getId() : existingId);
		}
		
	}

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

	private String updateMappingTable(ParticipantResponse response, String staginParticipantId,
			String staginPartIdSource) {
		String successMessage = "";
		switch (response.getStatus()) {
			case ADDED :
				epicDao.insertMapping(response.getParticipantDetail().getId(), staginParticipantId, staginPartIdSource);
				successMessage = MESSAGE_PARTICIPANT_ADDED;
				added_count++;
				break;
			case MODIFIED :
				epicDao.updateMapping(response.getParticipantDetail().getId(), staginParticipantId, staginPartIdSource);
				successMessage = MESSAGE_PARTICIPANT_UPDATED;
				edited_count++;
				break;
			case MRNDELETED :
				epicDao.updateMapping(response.getParticipantDetail().getId(), staginParticipantId, staginPartIdSource);
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

//	private final String CHANGE_TYPE_DELETED = "DELETED";

	private final String CHANGE_TYPE_MERGE = "MERGE";

	private final String CHANGE_TYPE_UNMERGE = "UNMERGE";

	private final String CHANGE_TYPE_CHANGE = "CHANGE";

	private final String MESSAGE_PARTICIPANT_ADDED = "Participant Added";

	private final String MESSAGE_PARTICIPANT_UPDATED = "Participant UPDATED";

	private final String MESSAGE_MRN_DELETED = "Participant MRN DELETED";

	private final String MESSAGE_PARTICIPANT_FAILED = "Failed";
	
	private final String MESSAGE_SCENARIO_NOT_HANDLED = "Scenario not handled";

}
