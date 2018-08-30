package com.krishagni.os.jhuprinttoken;

import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;

public class ParticipantRacePrintToken extends AbstractLabelTmplToken implements LabelTmplToken {

	@Override
	public String getName() {
		return "participant_race";
	}

	@Override
	public String getReplacement(Object object) {
		Specimen specimen = (Specimen) object;
		
		Set<String> raceSet = specimen.getRegistration().getParticipant().getRaces();
		String races = raceSet.size() > 0 ? raceSet.toString() : "";
		
		return StringUtils.isNotBlank(races) ? races : StringUtils.EMPTY;
	}
	
}
