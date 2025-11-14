package com.krishagni.openspecimen.le.support;

import java.util.Map;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.PlatformTransactionManager;

public class TxProbe implements ApplicationContextAware, InitializingBean {
    private ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext ctx) { this.ctx = ctx; }

    @Override
    public void afterPropertiesSet() {
        Map<String, PlatformTransactionManager> tms =
                ctx.getBeansOfType(PlatformTransactionManager.class);
        System.out.println("== TX MANAGERS ==");
        for (Map.Entry<String, PlatformTransactionManager> e : tms.entrySet()) {
            System.out.println("TM bean: " + e.getKey() + " -> " + e.getValue().getClass().getName());
        }
    }
}