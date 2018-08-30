package com.krishagni.os.jhuprinttoken;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.IntervalUnit;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;

public class VisitEventPointUnitPrintToken extends AbstractLabelTmplToken implements LabelTmplToken {

	@Override
	public String getName() {
		return "visit_event_point_unit";
	}

	@Override
	public String getReplacement(Object object) {
		IntervalUnit eventPoint;
		
		if (object instanceof Visit) {
			eventPoint =  ((Visit)object).getCpEvent().getEventPointUnit();
			return eventPoint != null ? eventPoint.toString() : "";
		} else if (object instanceof Specimen) {
			eventPoint = ((Specimen)object).getVisit().getCpEvent().getEventPointUnit();
			return eventPoint != null ? eventPoint.toString() : "";
		}
		
		return "";
	}

}
