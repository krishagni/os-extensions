package com.krishagni.os.jhuprinttoken;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;

public class VisitEventLabelPrintToken extends AbstractLabelTmplToken implements LabelTmplToken {

	@Override
	public String getName() {
		return "visit_event_label";
	}

	@Override
	public String getReplacement(Object object) {
		if (object instanceof Visit) {
			return ((Visit)object).getCpEvent().getEventLabel();
		} else if (object instanceof Specimen) {
			return ((Specimen)object).getVisit().getCpEvent().getEventLabel();
		}
		
		return "";
	}

}
