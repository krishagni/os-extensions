package com.krishagni.os.jhuprinttoken;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;

public class ParticipantDOBPrintToken extends AbstractLabelTmplToken implements LabelTmplToken {

	@Override
	public String getName() {
		return "participant_dob";
	}

	@Override
	public String getReplacement(Object object) {
		Specimen specimen = (Specimen) object;
		Date date = specimen.getRegistration().getParticipant().getBirthDate();
		String dob = date != null ? new SimpleDateFormat("dd-MM-yyyy").format(date) : "";
		
		return StringUtils.isNotBlank(dob) ? dob.toString() : StringUtils.EMPTY;
	}

}
