package com.krishagni.openspecimen.msk2.printtoken;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;

public class ParticipantExternalIdPrintToken extends AbstractLabelTmplToken implements LabelTmplToken {

	@Override
	public String getName() {
		return "msk2_participant_external_id";
	}

	@Override
	public String getReplacement(Object object) {
		String extId = null;
		
		if (object instanceof Visit) {
			extId = ((Visit)object).getRegistration().getExternalSubjectId();
		} else if (object instanceof Specimen) {
			extId = ((Specimen)object).getRegistration().getExternalSubjectId();
		}
		
		return StringUtils.isNotBlank(extId) ? extId : StringUtils.EMPTY;
	}

}
