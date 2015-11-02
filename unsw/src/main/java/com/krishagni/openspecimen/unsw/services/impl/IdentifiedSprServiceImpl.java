package com.krishagni.openspecimen.unsw.services.impl;

import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.EntityFormRecords;
import com.krishagni.catissueplus.core.de.events.FormCtxtSummary;
import com.krishagni.catissueplus.core.de.events.FormDataDetail;
import com.krishagni.catissueplus.core.de.events.FormRecordSummary;
import com.krishagni.catissueplus.core.de.events.GetEntityFormRecordsOp;
import com.krishagni.catissueplus.core.de.events.GetFormDataOp;
import com.krishagni.catissueplus.core.de.events.ListEntityFormsOp;
import com.krishagni.catissueplus.core.de.events.ListEntityFormsOp.EntityType;
import com.krishagni.catissueplus.core.de.services.FormService;
import com.krishagni.openspecimen.unsw.events.IdentifiedSprDetail;
import com.krishagni.openspecimen.unsw.services.IdentifiedSprService;

public class IdentifiedSprServiceImpl implements IdentifiedSprService {
	
	private FormService formSvc;
	
	public void setFormSvc(FormService formSvc) {
		this.formSvc = formSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<IdentifiedSprDetail> getIdentifiedSpr(RequestEvent<Long> req) {
		Long visitId = req.getPayload();
		IdentifiedSprDetail identifiedSprDetail = new IdentifiedSprDetail();
		int noOfRecords = 0; 
		
		List<FormCtxtSummary> formContexts = getVisitForms(visitId);
		for (FormCtxtSummary formCtxtSummary : formContexts) {
			if (formCtxtSummary.getFormCaption().equals(IDENTIFIED_SPR_FORM)) {
				identifiedSprDetail.setFormId(formCtxtSummary.getFormId());
				identifiedSprDetail.setFormContextId(formCtxtSummary.getFormCtxtId());
				noOfRecords = formCtxtSummary.getNoOfRecords();
				break;
			}
		}
		
		if (noOfRecords > 0) {
			identifiedSprDetail.setRecordId(getFormRecordId(visitId, identifiedSprDetail.getFormContextId()));
			Map<String, Object> formData = getFormData(identifiedSprDetail.getFormId(), identifiedSprDetail.getRecordId());
			identifiedSprDetail.setFormData(formData);
		}
		
		return ResponseEvent.response(identifiedSprDetail);
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
		FormRecordSummary formRecordSummary = entityFormRecords.getRecords().get(0);
		return formRecordSummary.getRecordId();
	}

	private Map<String, Object> getFormData(Long formId, Long recordId) {
		GetFormDataOp opDetail = new GetFormDataOp();
		opDetail.setFormId(formId);
		opDetail.setRecordId(recordId);
		ResponseEvent<FormDataDetail> resp = formSvc.getFormData(getRequest(opDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload().getFormData().getFieldNameValueMap(false);
	}
	
	private <T> RequestEvent<T> getRequest(T payload) {
		return new RequestEvent<T>(payload);				
	}
	
	private static final String IDENTIFIED_SPR_FORM = "Identified Surgical Pathology Report";

}
