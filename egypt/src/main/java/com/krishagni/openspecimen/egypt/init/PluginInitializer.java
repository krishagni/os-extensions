package com.krishagni.openspecimen.egypt.init;

import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
import com.krishagni.openspecimen.egypt.token.PpidUniqueIdLabelToken;


public class PluginInitializer implements InitializingBean {
  
	private LabelTmplTokenRegistrar ppidTokensRegistrar;
   
	public void setPpidTokensRegistrar(LabelTmplTokenRegistrar ppidTokensRegistrar) {
		this.ppidTokensRegistrar = ppidTokensRegistrar;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		ppidTokensRegistrar.register(new PpidUniqueIdLabelToken());
  }
}