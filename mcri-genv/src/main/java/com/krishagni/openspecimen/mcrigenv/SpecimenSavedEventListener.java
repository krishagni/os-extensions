package com.krishagni.openspecimen.mcrigenv;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenSavedEvent;

public class SpecimenSavedEventListener implements ApplicationListener<SpecimenSavedEvent> {
    private static final Log logger = LogFactory.getLog(SpecimenSavedEventListener.class);

    @Override
    public void onApplicationEvent(SpecimenSavedEvent event) {
        Specimen specimen = (Specimen) event.getEventData();
        logger.info(specimen.toAuditString());
    }
}
