package com.krishagni.os.jhuprinttoken;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;

public class CollectionProtocolPIPrintToken extends AbstractLabelTmplToken implements LabelTmplToken {

	@Override
	public String getName() {
		return "collection_protocol_principal_investigator";
	}

	@Override
	public String getReplacement(Object object) {
		User pI;
		
		if (object instanceof Visit) {
			pI = ((Visit)object).getCollectionProtocol().getPrincipalInvestigator();
			//return pI.getFirstName() + " " + pI.getLastName();
			return "Can't Access";
		} else if (object instanceof Specimen) {
			pI = ((Specimen)object).getCollectionProtocol().getPrincipalInvestigator();
			//return pI.getFirstName() + " " + pI.getLastName();
			return "Can't Access";
		}
		
		return "";
	}

}
