package com.krishagni.openspecimen.qlh.biospecimen;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationListener;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenReceivedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenSavedEvent;

public class SpecimenSavedEventHandler implements ApplicationListener<SpecimenSavedEvent> {
	private static final Logger logger = LogManager.getLogger(SpecimenSavedEventHandler.class);

	@Override
	public void onApplicationEvent(SpecimenSavedEvent event) {
		Specimen specimen = event.getEventData();
		if (!specimen.isUpdated() || !specimen.isPrimary() || !specimen.isCollected()) {
			return;
		}

		SpecimenReceivedEvent receivedEvent = specimen.getReceivedEvent();
		if (logger.isDebugEnabled()) {
			logger.debug("Update specimen " + specimen.getLabel() + " created on date/time to " + receivedEvent.getTime().toString());
		}

		specimen.setCreatedOn(receivedEvent.getTime());
		specimen.setUpdated(true);
	}
}
