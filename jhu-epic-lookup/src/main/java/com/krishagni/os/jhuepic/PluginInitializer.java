package com.krishagni.os.jhuepic;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.biospecimen.WorkflowUtil;
import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig;
import com.krishagni.catissueplus.core.common.domain.LabelTmplTokenRegistrar;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.os.jhuprinttoken.CollectionProtocolPIPrintToken;
import com.krishagni.os.jhuprinttoken.ParticipantDOBPrintToken;
import com.krishagni.os.jhuprinttoken.ParticipantFirstNamePrintToken;
import com.krishagni.os.jhuprinttoken.ParticipantGenderPrintToken;
import com.krishagni.os.jhuprinttoken.ParticipantLastNamePrintToken;
import com.krishagni.os.jhuprinttoken.ParticipantRacePrintToken;
import com.krishagni.os.jhuprinttoken.VisitEventLabelPrintToken;
import com.krishagni.os.jhuprinttoken.VisitEventPointPrintToken;
import com.krishagni.os.jhuprinttoken.VisitEventPointUnitPrintToken;
import com.krishagni.os.jhuprinttoken.VisitSurgPathPrintToken;

public class PluginInitializer implements InitializingBean {
	private static final Log logger = LogFactory.getLog(PluginInitializer.class);

	private ConfigurationService cfgSvc;
	
	private LabelTmplTokenRegistrar labelTokensRegistrar;

	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}
	
	public void setLabelTokensRegistrar(LabelTmplTokenRegistrar labelTokensRegistrar) {
		this.labelTokensRegistrar = labelTokensRegistrar;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		labelTokensRegistrar.register(new ParticipantFirstNamePrintToken());
		labelTokensRegistrar.register(new ParticipantLastNamePrintToken());
		labelTokensRegistrar.register(new ParticipantDOBPrintToken());
		labelTokensRegistrar.register(new ParticipantGenderPrintToken());
		labelTokensRegistrar.register(new ParticipantRacePrintToken());
		labelTokensRegistrar.register(new VisitEventLabelPrintToken());
		labelTokensRegistrar.register(new VisitSurgPathPrintToken());
		labelTokensRegistrar.register(new VisitEventPointPrintToken());
		labelTokensRegistrar.register(new VisitEventPointUnitPrintToken());
		labelTokensRegistrar.register(new CollectionProtocolPIPrintToken());
		
		CpWorkflowConfig sysWorkflows = WorkflowUtil.getInstance().getSysWorkflows();
		CpWorkflowConfig.Workflow workflow = sysWorkflows.getWorkflows().get("locked-fields");
		if (workflow == null) {
			InputStream in = null;
			try {
				in = getClass().getResourceAsStream(LOCKED_FIELDS_CFG);
				workflow = new ObjectMapper().readValue(in, CpWorkflowConfig.Workflow.class);
				sysWorkflows.getWorkflows().put("locked-fields", workflow);
			} catch (Exception e) {
				logger.error("Error initialising locked fields configuration", e);
			} finally {
				IOUtils.closeQuietly(in);
			}
		}
	}
	
	private static final String LOCKED_FIELDS_CFG = "/com/krishagni/os/jhuepic/locked-fields.json";
}