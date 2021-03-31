package com.krishagni.openspecimen.eventlistener;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenSavedEvent;

@Configuration
public class SpecimenSavedEventListener implements ApplicationListener<SpecimenSavedEvent>{
	private static final Logger logger = Logger.getLogger(SpecimenSavedEventListener.class);
	@Override
	public void onApplicationEvent(SpecimenSavedEvent event) {
		Specimen specimen = (Specimen) event.getSource();
		logger.info(specimen.toAuditString());
	}
}
