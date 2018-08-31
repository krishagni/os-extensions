package com.krishagni.os.jhuprinttoken;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;

public class VisitSurgPathPrintToken extends AbstractLabelTmplToken implements LabelTmplToken {

	@Override
	public String getName() {
		return "jhu_visit_surg_path";
	}

	@Override
	public String getReplacement(Object object) {
		if (object instanceof Visit) {
			return ((Visit)object).getSurgicalPathologyNumber();
		} else if (object instanceof Specimen) {
			return ((Specimen)object).getVisit().getSurgicalPathologyNumber();
		}
		
		return "";
	}

}
