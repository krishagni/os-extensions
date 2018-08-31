package com.krishagni.os.jhuprinttoken;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;

public class VisitEventPointPrintToken extends AbstractLabelTmplToken implements LabelTmplToken {

	@Override
	public String getName() {
		return "jhu_visit_event_point";
	}

	@Override
	public String getReplacement(Object object) {
		CollectionProtocolEvent cpEvent = null;
		
		if (object instanceof Visit) {
			cpEvent = ((Visit)object).getCpEvent();
			if (cpEvent == null) {
				return "";
			}
			return cpEvent.getEventPoint() != null ? cpEvent.getEventPoint().toString() : "";
		} else if (object instanceof Specimen) {
			cpEvent = ((Specimen)object).getVisit().getCpEvent();
			if (cpEvent == null) {
				return "";
			}
			return cpEvent.getEventPoint() != null ? cpEvent.getEventPoint().toString() : "";
		}
		
		return "";
	}

}
