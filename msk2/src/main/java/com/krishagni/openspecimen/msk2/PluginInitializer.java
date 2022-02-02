package com.krishagni.openspecimen.msk2;

import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
import com.krishagni.openspecimen.msk2.printtoken.ParticipantExternalIdPrintToken;
import com.krishagni.openspecimen.msk2.printtoken.VisitEventLabelPrintToken;

public class PluginInitializer implements InitializingBean {
	private LabelTmplTokenRegistrar labelTokensRegistrar;

	public void setLabelTokensRegistrar(LabelTmplTokenRegistrar labelTokensRegistrar) {
		this.labelTokensRegistrar = labelTokensRegistrar;
	}

	@Override
	public void afterPropertiesSet()
	throws Exception {
		Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

		labelTokensRegistrar.register(new VisitEventLabelPrintToken());
		labelTokensRegistrar.register(new ParticipantExternalIdPrintToken());
	}
}
