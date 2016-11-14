package com.krishagni.openspecimen.uoa.print;

import org.springframework.beans.factory.InitializingBean;
import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;

public class PluginInitializer implements InitializingBean {

	private LabelTmplTokenRegistrar specimenPrintLabelTokensRegistrar;

	public void setSpecimenPrintLabelTokensRegistrar(LabelTmplTokenRegistrar specimenPrintLabelTokensRegistrar) {
		this.specimenPrintLabelTokensRegistrar = specimenPrintLabelTokensRegistrar;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		specimenPrintLabelTokensRegistrar.register(new SpecimenMpiPrintToken());
	}
}
