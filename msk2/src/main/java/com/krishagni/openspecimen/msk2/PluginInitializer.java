package com.krishagni.openspecimen.msk2;

import org.springframework.beans.factory.InitializingBean;

public class PluginInitializer implements InitializingBean {
	@Override
	public void afterPropertiesSet()
	throws Exception {
		Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
	}
}
