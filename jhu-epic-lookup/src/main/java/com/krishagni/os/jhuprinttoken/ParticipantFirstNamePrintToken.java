package com.krishagni.os.jhuprinttoken;

import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.label.specimen.AbstractSpecimenLabelToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;

public class ParticipantFirstNamePrintToken extends AbstractSpecimenLabelToken implements LabelTmplToken {

	@Override
	public String getName() {
		return "participant_first_name";
	}

	@Override
	public String getReplacement(Object object) {
		Specimen specimen = (Specimen) object;
		
		while (specimen.getParentSpecimen() != null) {
            		specimen = specimen.getParentSpecimen();
        	}
		
		if (StringUtils.isBlank(specimen.getRegistration().getParticipant().getFirstName())) {
			return StringUtils.EMPTY;
		}
		
		return specimen.getRegistration().getParticipant().getFirstName();
	}

	@Override
	public String getLabel(Specimen specimen) {
		return "";
	}
}
