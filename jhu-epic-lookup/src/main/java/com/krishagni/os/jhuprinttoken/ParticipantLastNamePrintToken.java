package com.krishagni.os.jhuprinttoken;

import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;

public class ParticipantLastNamePrintToken extends AbstractLabelTmplToken implements LabelTmplToken {

	@Override
	public String getName() {
		return "participant_last_name";
	}

	@Override
	public String getReplacement(Object object) {
		Specimen specimen = (Specimen) object;
		String name = specimen.getRegistration().getParticipant().getLastName();
		
		return StringUtils.isNotBlank(name) ? name : StringUtils.EMPTY;
	}
	
}
