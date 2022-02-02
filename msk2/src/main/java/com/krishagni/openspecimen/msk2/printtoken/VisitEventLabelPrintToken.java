package com.krishagni.openspecimen.msk2.printtoken;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;

public class VisitEventLabelPrintToken extends AbstractLabelTmplToken implements LabelTmplToken {

	@Override
	public String getName() {
		return "msk2_visit_event_label";
	}

	@Override
	public String getReplacement(Object object) {
		CollectionProtocolEvent cpEvent = null;
		
		if (object instanceof Visit) {
			cpEvent = ((Visit)object).getCpEvent();
		} else if (object instanceof Specimen) {
			cpEvent = ((Specimen)object).getVisit().getCpEvent();
		}
		
		return cpEvent != null ? cpEvent.getEventLabel() : "";
	}

}
