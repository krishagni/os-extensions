package com.krishagni.os.jhuprinttoken;

import java.util.Set;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.util.Utility;

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
		
		return Utility.nullSafeStream(raceSet).collect(Collectors.joining(","));
	}
}
