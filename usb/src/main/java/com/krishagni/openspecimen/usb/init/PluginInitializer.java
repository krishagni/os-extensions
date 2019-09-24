package com.krishagni.openspecimen.usb.init;

import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
import com.krishagni.openspecimen.usb.token.SpecimenLISGermCodePrintToken;

public class PluginInitializer implements InitializingBean {
	private LabelTmplTokenRegistrar specimenPrintLabelTokensRegistrar;

	private SpecimenLISGermCodePrintToken lisGermCodePrintToken;

	public void setSpecimenPrintLabelTokensRegistrar(LabelTmplTokenRegistrar specimenPrintLabelTokensRegistrar) {
		this.specimenPrintLabelTokensRegistrar = specimenPrintLabelTokensRegistrar;
	}

	public void setLisGermCodePrintToken(SpecimenLISGermCodePrintToken lisGermCodePrintToken) {
		this.lisGermCodePrintToken = lisGermCodePrintToken;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		specimenPrintLabelTokensRegistrar.register(lisGermCodePrintToken);
	}
}
