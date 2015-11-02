package com.krishagni.openspecimen.unsw.services.impl;

import java.util.List;

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

public class IdentifiedSprServiceImpl implements IdentifiedSprService{
	
	private FormService formSvc;
	
	public void setFormSvc(FormService formSvc) {
		this.formSvc = formSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<IdentifiedSprDetail> getIdentifiedSprDetail(RequestEvent<Long> req) {
		Long visitId = req.getPayload();
		ListEntityFormsOp opDetail = new ListEntityFormsOp();
		opDetail.setEntityId(visitId);
		opDetail.setEntityType(EntityType.SPECIMEN_COLLECTION_GROUP);
		
		ResponseEvent<List<FormCtxtSummary>> resp = formSvc.getEntityForms(getRequest(opDetail));
		
		List<FormCtxtSummary> formContexts = resp.getPayload();
		IdentifiedSprDetail identifiedSprDetail = new IdentifiedSprDetail();
		int noOfRecords = 0; 
		for (FormCtxtSummary formCtxtSummary : formContexts) {
			if (formCtxtSummary.getFormCaption().equals("Identified SPR")) {
				identifiedSprDetail.setFormId(formCtxtSummary.getFormId());
				identifiedSprDetail.setFormContextId(formCtxtSummary.getFormCtxtId());
				noOfRecords = formCtxtSummary.getNoOfRecords();
				break;
			}
		}
		
		if (noOfRecords > 0) {
			GetEntityFormRecordsOp entityFormRecordsOpDetail = new GetEntityFormRecordsOp();
			entityFormRecordsOpDetail.setEntityId(visitId);
			entityFormRecordsOpDetail.setFormCtxtId(identifiedSprDetail.getFormContextId());

			ResponseEvent<EntityFormRecords> resp1 = formSvc.getEntityFormRecords(getRequest(entityFormRecordsOpDetail));
			EntityFormRecords entityFormRecords = resp1.getPayload();
			FormRecordSummary formRecordSummary = entityFormRecords.getRecords().get(0);
			identifiedSprDetail.setRecordId(formRecordSummary.getRecordId());
			
			GetFormDataOp op = new GetFormDataOp();
			op.setFormId(identifiedSprDetail.getFormId());
			op.setRecordId(identifiedSprDetail.getRecordId());
			
			ResponseEvent<FormDataDetail> resp2 = formSvc.getFormData(getRequest(op));
			identifiedSprDetail.setFormData(resp2.getPayload().getFormData().getFieldNameValueMap(true));
		}
		
		return ResponseEvent.response(identifiedSprDetail);
	}
	
	private <T> RequestEvent<T> getRequest(T payload) {
		return new RequestEvent<T>(payload);				
	}

}
