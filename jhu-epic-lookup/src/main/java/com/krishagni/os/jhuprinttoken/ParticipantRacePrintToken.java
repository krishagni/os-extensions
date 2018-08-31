package com.krishagni.os.jhuprinttoken;

import java.util.Iterator;
import java.util.Set;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;

public class ParticipantRacePrintToken extends AbstractLabelTmplToken implements LabelTmplToken {

	@Override
	public String getName() {
		return "jhu_participant_race";
	}

	@Override
	public String getReplacement(Object object) {
		Set<String> raceSet = null;
		
		if (object instanceof Visit) {
			raceSet = ((Visit)object).getRegistration().getParticipant().getRaces();
		} else if (object instanceof Specimen) {
			raceSet = ((Specimen)object).getRegistration().getParticipant().getRaces();
		}
		
		return raceSet.size() > 0 ? raceSetJoiner(raceSet) : "";
	}

	private String raceSetJoiner(Set<String> raceSet) {
		Iterator<String> iterator = raceSet.iterator();
		StringBuilder races = new StringBuilder(iterator.next());
		iterator.forEachRemaining(race -> races.append(", ").append(race));
		
		return races.toString();
	}
	
}
