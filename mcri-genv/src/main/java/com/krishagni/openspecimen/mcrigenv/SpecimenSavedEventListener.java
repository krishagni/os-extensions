package com.krishagni.openspecimen.mcrigenv;

import org.springframework.context.ApplicationListener;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenSavedEvent;
import com.krishagni.openspecimen.mcrigenv.jms.JmsMessagePublisher;

public class SpecimenSavedEventListener implements ApplicationListener<SpecimenSavedEvent> {
    @Override
    public void onApplicationEvent(SpecimenSavedEvent event) {
    	JmsMessagePublisher messagePublisher = new JmsMessagePublisher();
    	messagePublisher.processMessage(event.getEventData());
    }
}
