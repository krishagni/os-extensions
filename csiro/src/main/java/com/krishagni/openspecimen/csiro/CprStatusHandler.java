package com.krishagni.openspecimen.csiro;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationListener;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.CprSavedEvent;
import com.krishagni.catissueplus.core.de.domain.DeObject;

public class CprStatusHandler implements ApplicationListener<CprSavedEvent> {
	@Override
	public void onApplicationEvent(CprSavedEvent event) {
		CollectionProtocolRegistration cpr = event.getEventData();
		DeObject customFields = cpr.getExtension();

		Object status = getAttrValue(customFields, "user_consent_status");
		if (status != null) {
			setAttrValue(customFields, "consent_status", status);
			return;
		}

		System.err.println("Participant: " + cpr.getPpid() + " saved!");
	}

	private Object getAttrValue(DeObject customFields, String name) {
		DeObject.Attr attr = getAttr(customFields, name);
		return attr != null ? attr.getValue() : null;
	}

	private DeObject.Attr getAttr(DeObject customFields, String name) {
		if (customFields == null || customFields.getAttrs() == null || StringUtils.isBlank(name)) {
			return null;
		}

		DeObject.Attr resultAttr = null;
		for (DeObject.Attr attr : customFields.getAttrs()) {
			if (attr.getName().equals(name) || attr.getUdn().equals(name)) {
				resultAttr = attr;
				break;
			}
		}

		return resultAttr;
	}

	private void setAttrValue(DeObject customFields, String name, Object value) {
		DeObject.Attr resultAttr = getAttr(customFields, name);
		if (resultAttr == null) {
			resultAttr = new DeObject.Attr();
			resultAttr.setName(name);
			resultAttr.setUdn(name);
			customFields.getAttrs().add(resultAttr);
		}

		resultAttr.setValue(value);
	}

}
