package com.krishagni.openspecimen.csiro;

import org.springframework.context.ApplicationListener;

import com.krishagni.catissueplus.core.administrative.domain.FormDataSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;

import edu.common.dynamicextensions.napi.FormData;

public class FormDataStatusHandler implements ApplicationListener<FormDataSavedEvent> {
	private CprStatusHandler cprStatusHandler;

	public void setCprStatusHandler(CprStatusHandler cprStatusHandler) {
		this.cprStatusHandler = cprStatusHandler;
	}

	@Override
	public void onApplicationEvent(FormDataSavedEvent event) {
		if (!(event.getObject() instanceof CollectionProtocolRegistration) || !"Participant".equals(event.getEntityType())) {
			return;
		}

		FormData formData = event.getEventData();
		String formName = formData.getContainer().getName();
		if (!formName.equals("nhms_participant_consent_form")) {
			return;
		}

		cprStatusHandler.handleStatus((CollectionProtocolRegistration) event.getObject(), formData);
	}
}
