package com.krishagni.openspecimen.indiana;

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationListener;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.CprSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.ParticipantMedicalIdentifier;

public class CprSaveHandler implements ApplicationListener<CprSavedEvent> {
	@Override
	public void onApplicationEvent(CprSavedEvent event) {
		CollectionProtocolRegistration cpr = event.getEventData();
		Participant participant = cpr.getParticipant();

		boolean found = false;
		Iterator<ParticipantMedicalIdentifier> pmiIter = participant.getPmis().iterator();
		while (pmiIter.hasNext()) {
			ParticipantMedicalIdentifier pmi = pmiIter.next();
			if (pmi.getSite().equals(cpr.getSite())) {
				found = true;
			} else if (StringUtils.isBlank(pmi.getMedicalRecordNumber())) {
				pmiIter.remove();
			}
		}

		if (!found && cpr.getSite() != null) {
			ParticipantMedicalIdentifier pmi = new ParticipantMedicalIdentifier();
			pmi.setParticipant(participant);
			pmi.setSite(cpr.getSite());
			participant.getPmis().add(pmi);
		}
	}
}
