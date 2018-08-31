package com.krishagni.os.jhuprinttoken;

import org.apache.commons.lang.StringUtils;

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
		String gender = null;
		
		if (object instanceof Visit) {
			gender = ((Visit)object).getRegistration().getParticipant().getGender();
		} else if (object instanceof Specimen) {
			gender = ((Specimen)object).getRegistration().getParticipant().getGender();
		}
		
		return StringUtils.isNotBlank(gender) ? gender : StringUtils.EMPTY;
	}
	
}
