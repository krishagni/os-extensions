package com.krishagni.openspecimen.msk;

import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.administrative.services.DistributionOrderService;

public class PluginInitializer implements InitializingBean {
	private DistributionOrderService orderSvc;

	public void setOrderSvc(DistributionOrderService orderSvc) {
		this.orderSvc = orderSvc;
	}

	@Override
	public void afterPropertiesSet()
	throws Exception {
		orderSvc.addValidator(new ConsentValidator());
	}
}
