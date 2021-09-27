package com.krishagni.os.jhuprinttoken;

import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
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
		CollectionProtocolRegistration cpr = null;
		if (object instanceof Visit) {
			cpr = ((Visit) object).getRegistration();
		} else if (object instanceof Specimen) {
			cpr = ((Specimen) object).getRegistration();
		}

		if (cpr == null) {
			return StringUtils.EMPTY;
		}

		return Utility.nullSafeStream(cpr.getParticipant().getRaces())
				.map(race -> race.getValue())
				.collect(Collectors.joining(","));
	}
}
