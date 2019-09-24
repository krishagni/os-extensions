package com.krishagni.openspecimen.usb.init;

import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
import com.krishagni.openspecimen.usb.token.SpecimenLISDayNumberPrintToken;
import com.krishagni.openspecimen.usb.token.SpecimenLISGermCodePrintToken;

public class PluginInitializer implements InitializingBean {
	private LabelTmplTokenRegistrar specimenPrintLabelTokensRegistrar;

	private SpecimenLISGermCodePrintToken lisGermCodePrintToken;

	private SpecimenLISDayNumberPrintToken lisDayNumberPrintToken;

	public void setSpecimenPrintLabelTokensRegistrar(LabelTmplTokenRegistrar specimenPrintLabelTokensRegistrar) {
		this.specimenPrintLabelTokensRegistrar = specimenPrintLabelTokensRegistrar;
	}

	public void setLisGermCodePrintToken(SpecimenLISGermCodePrintToken lisGermCodePrintToken) {
		this.lisGermCodePrintToken = lisGermCodePrintToken;
	}

	public void setLisDayNumberPrintToken(SpecimenLISDayNumberPrintToken lisDayNumberPrintToken) {
		this.lisDayNumberPrintToken = lisDayNumberPrintToken;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		specimenPrintLabelTokensRegistrar.register(lisGermCodePrintToken);
		specimenPrintLabelTokensRegistrar.register(lisDayNumberPrintToken);
	}
}
