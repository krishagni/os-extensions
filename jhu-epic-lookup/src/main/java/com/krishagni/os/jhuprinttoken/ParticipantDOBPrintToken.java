package com.krishagni.os.jhuprinttoken;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

public class ParticipantDOBPrintToken extends AbstractLabelTmplToken implements LabelTmplToken {
	
	@Override
	public String getName() {
		return "jhu_participant_dob";
	}

	@Override
	public String getReplacement(Object object) {
		Date date = null;
		
		if (object instanceof Visit) {
			date = ((Visit)object).getRegistration().getParticipant().getBirthDate();
		} else if (object instanceof Specimen) {
			date = ((Specimen)object).getRegistration().getParticipant().getBirthDate();
		}
		
		return date != null ? new SimpleDateFormat(ConfigUtil.getInstance().getDateFmt()).format(date) : "";
	}

}
