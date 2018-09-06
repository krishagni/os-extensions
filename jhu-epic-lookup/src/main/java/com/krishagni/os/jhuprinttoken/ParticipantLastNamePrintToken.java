package com.krishagni.os.jhuprinttoken;

import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;

public class ParticipantLastNamePrintToken extends AbstractLabelTmplToken implements LabelTmplToken {

	@Override
	public String getName() {
		return "jhu_participant_last_name";
	}

	@Override
	public String getReplacement(Object object) {
		String name = null;
		
		if (object instanceof Visit) {
			name = ((Visit)object).getRegistration().getParticipant().getLastName();
		} else if (object instanceof Specimen) {
			name = ((Specimen)object).getRegistration().getParticipant().getLastName();
		}
		
		return StringUtils.isNotBlank(name) ? name : StringUtils.EMPTY;
	}
	
}
