package com.krishagni.openspecimen.egypt.token;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.label.cpr.AbstractPpidToken;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;

@Configurable
public class PpidUniqueIdLabelToken extends AbstractPpidToken {

	@Autowired
	private DaoFactory biospecimenDaoFactory;
	
	public PpidUniqueIdLabelToken() {
		this.name = "SYS_UID";
	}
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.biospecimenDaoFactory = daoFactory;
	}

	@Override
	public String getLabel(CollectionProtocolRegistration arg0, String... arg1) {
		Long uniqueId = biospecimenDaoFactory.getUniqueIdGenerator().getUniqueId("Registration", getName());
		return uniqueId.toString();
	}

}
