package com.krishagni.os.jhuprinttoken;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;

public class ParticipantFirstNamePrintToken extends AbstractLabelTmplToken implements LabelTmplToken {

	@Override
	public String getName() {
		return "jhu_participant_first_name";
	}

	@Override
	public String getReplacement(Object object) {
		String name = null;
		
		if (object instanceof Visit) {
			name = ((Visit)object).getRegistration().getParticipant().getFirstName();
		} else if (object instanceof Specimen) {
			name = ((Specimen)object).getRegistration().getParticipant().getFirstName();
		}
		
		return StringUtils.isNotBlank(name) ? name : StringUtils.EMPTY;
	}
}
