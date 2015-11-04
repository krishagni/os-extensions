package com.krishagni.openspecimen.unsw.services.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.EntityFormRecords;
import com.krishagni.catissueplus.core.de.events.FileDetail;
import com.krishagni.catissueplus.core.de.events.FormCtxtSummary;
import com.krishagni.catissueplus.core.de.events.FormDataDetail;
import com.krishagni.catissueplus.core.de.events.GetEntityFormRecordsOp;
import com.krishagni.catissueplus.core.de.events.GetFileDetailOp;
import com.krishagni.catissueplus.core.de.events.GetFormDataOp;
import com.krishagni.catissueplus.core.de.events.ListEntityFormsOp;
import com.krishagni.catissueplus.core.de.events.ListEntityFormsOp.EntityType;
import com.krishagni.catissueplus.core.de.services.FormService;
import com.krishagni.openspecimen.unsw.domain.factory.IdentifiedSprErrorCode;
import com.krishagni.openspecimen.unsw.events.IdentifiedSprDetail;
import com.krishagni.openspecimen.unsw.services.IdentifiedSprService;

import edu.common.dynamicextensions.napi.FormData;


public class IdentifiedSprServiceImpl implements IdentifiedSprService {
	
	private FormService formSvc;
	
	public void setFormSvc(FormService formSvc) {
		this.formSvc = formSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<String> getIdentifiedSprName(RequestEvent<Long> req) {
		Long visitId = req.getPayload();
		FormCtxtSummary sprFormCtxtSummary = getSprFormContext(visitId);
		String identifiedSprName = null;
		
		if (sprFormCtxtSummary.getNoOfRecords() > 0) {
			Long recordId = getFormRecordId(visitId, sprFormCtxtSummary.getFormCtxtId());
			Map<String, Object> formData = getFormData(sprFormCtxtSummary.getFormId(), recordId);
			Map<Object, Object> fileDetail = (Map<Object, Object>) formData.get("fileUpload");
			identifiedSprName = fileDetail.get("filename").toString();
		}
		
		return ResponseEvent.response(identifiedSprName);
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<FileDetail> getIdentifiedSpr(RequestEvent<Long> req) {
		Long visitId = req.getPayload();
		FormCtxtSummary sprFormContext = getSprFormContext(visitId);
		if(sprFormContext.getNoOfRecords() == 0) {
			throw OpenSpecimenException.userError(IdentifiedSprErrorCode.NOT_FOUND);
		}
		
		Long recordId = getFormRecordId(visitId, sprFormContext.getFormCtxtId());
		GetFileDetailOp op = new GetFileDetailOp();
		op.setFormId(sprFormContext.getFormId());
		op.setRecordId(recordId);
		op.setCtrlName("fileUpload");
		return formSvc.getFileDetail(getRequest(op));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<FileDetail> uploadIdentifiedSpr(RequestEvent<IdentifiedSprDetail> req) {
		IdentifiedSprDetail detail = req.getPayload();
		ResponseEvent<FileDetail> resp = formSvc.uploadFile(getRequest(detail.getSpr()));
		resp.throwErrorIfUnsuccessful();
		
		// Save record 
		FileDetail sprFileDetail = resp.getPayload();
		FormCtxtSummary sprFormContext = getSprFormContext(detail.getVisitId());
		FormData formData = createFormData(detail.getVisitId(), sprFileDetail, sprFormContext);
		
		FormDataDetail formDataDetail = new FormDataDetail();
		formDataDetail.setFormId(sprFormContext.getFormId());
		formDataDetail.setFormData(formData);
		if (sprFormContext.getNoOfRecords() > 0) {
			Long recordId = getFormRecordId(detail.getVisitId(), sprFormContext.getFormCtxtId());
			formDataDetail.setRecordId(recordId);
		}
		
		ResponseEvent<FormDataDetail> response = formSvc.saveFormData(getRequest(formDataDetail));
		response.throwErrorIfUnsuccessful();
		return resp;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> deleteIdentifiedSpr(RequestEvent<Long> req) {
		Long visitId = req.getPayload();
		FormCtxtSummary sprFormContext = getSprFormContext(visitId);
		if(sprFormContext.getNoOfRecords() == 0) {
			throw OpenSpecimenException.userError(IdentifiedSprErrorCode.NOT_FOUND);
		}
		
		Long recordId = getFormRecordId(visitId, sprFormContext.getFormCtxtId());
		ResponseEvent<Long> resp = formSvc.deleteRecord(getRequest(recordId));
		resp.throwErrorIfUnsuccessful();
		return ResponseEvent.response(true);
	}
	
	private FormCtxtSummary getSprFormContext(Long visitId) {
		FormCtxtSummary sprFormContext = null;
		List<FormCtxtSummary> formContexts = getVisitForms(visitId);
		for (FormCtxtSummary formCtxtSummary : formContexts) {
			if (formCtxtSummary.getFormCaption().equals(IDENTIFIED_SPR_FORM)) {
				sprFormContext = formCtxtSummary;
				break;
			}
		}
		
		if (sprFormContext == null) {
			throw OpenSpecimenException.userError(IdentifiedSprErrorCode.FORM_NOT_FOUND);
		}
		
		return sprFormContext;
	}
	
	private List<FormCtxtSummary> getVisitForms(Long visitId) {
		ListEntityFormsOp opDetail = new ListEntityFormsOp();
		opDetail.setEntityId(visitId);
		opDetail.setEntityType(EntityType.SPECIMEN_COLLECTION_GROUP);
		ResponseEvent<List<FormCtxtSummary>> resp = formSvc.getEntityForms(getRequest(opDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	private Long getFormRecordId(Long visitId, Long formContextId) {
		GetEntityFormRecordsOp opDetail = new GetEntityFormRecordsOp();
		opDetail.setEntityId(visitId);
		opDetail.setFormCtxtId(formContextId);
		ResponseEvent<EntityFormRecords> resp = formSvc.getEntityFormRecords(getRequest(opDetail));
		resp.throwErrorIfUnsuccessful();
		EntityFormRecords entityFormRecords = resp.getPayload();
		return entityFormRecords.getRecords().get(0).getRecordId();
	}

	private Map<String, Object> getFormData(Long formId, Long recordId) {
		GetFormDataOp opDetail = new GetFormDataOp();
		opDetail.setFormId(formId);
		opDetail.setRecordId(recordId);
		ResponseEvent<FormDataDetail> resp = formSvc.getFormData(getRequest(opDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload().getFormData().getFieldNameValueMap(false);
	}
	
	private FormData createFormData(Long visitId, FileDetail sprFileDetail, FormCtxtSummary sprFormContext) {
		Map<String,Object> valueMap = new HashMap<String, Object>();
		
		Map<String,Object> appData = new HashMap<String, Object>();
		appData.put("formCtxtId", sprFormContext.getFormCtxtId());
		appData.put("objectId", visitId);
		
		Map<String,Object> fileUpload = new HashMap<String, Object>();
		fileUpload.put("filename", sprFileDetail.getFilename());
		fileUpload.put("contentType", sprFileDetail.getContentType());
		fileUpload.put("fileId", sprFileDetail.getFileId());
		
		valueMap.put("appData",appData);
		valueMap.put("fileUpload", fileUpload);
		
		return FormData.fromValueMap(sprFormContext.getFormId(), valueMap);
	}
	
	private <T> RequestEvent<T> getRequest(T payload) {
		return new RequestEvent<T>(payload);				
	}
	
	private static final String IDENTIFIED_SPR_FORM = "Identified Surgical Pathology Report";

}
