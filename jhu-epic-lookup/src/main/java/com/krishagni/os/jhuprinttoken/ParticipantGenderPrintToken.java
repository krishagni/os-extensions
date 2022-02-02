package com.krishagni.os.jhuprinttoken;

import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;

public class ParticipantGenderPrintToken extends AbstractLabelTmplToken implements LabelTmplToken {

	@Override
	public String getName() {
		return "jhu_participant_gender";
	}

	@Override
	public String getReplacement(Object object) {
		CollectionProtocolRegistration cpr = null;
		if (object instanceof Visit) {
			cpr = ((Visit) object).getRegistration();
		} else if (object instanceof Specimen) {
			cpr = ((Specimen) object).getRegistration();
		}

		if (cpr != null && cpr.getParticipant().getGender() != null) {
			return cpr.getParticipant().getGender().getValue();
		} else {
			return StringUtils.EMPTY;
		}
	}
}
