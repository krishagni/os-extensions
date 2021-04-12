package com.krishagni.openspecimen.emorylits;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenSavedEvent;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;

public class SpecimenSavedEventListener implements ApplicationListener<SpecimenSavedEvent>, InitializingBean {
    private static final Log logger = LogFactory.getLog(SpecimenSavedEventListener.class);

    private static final String MODULE = "biospecimen";

    private static final String CONFIG = "emory_lits_cfg";

    private ConfigurationService configSvc;

    private Config config;

    public void setConfigSvc(ConfigurationService configSvc) {
        this.configSvc = configSvc;
    }

    @Override
    public void onApplicationEvent(SpecimenSavedEvent event) {
        if (config == null) {
            logger.warn("Notifications setting is not configured yet. Nothing to do.");
            return;
        }

        Specimen specimen = event.getEventData();
        List<String> cps = config.getCps();
        if (cps != null && !cps.isEmpty() && !cps.contains(specimen.getCpShortTitle())) {
            return;
        }
        updateSpecimenBarcode(specimen);
    }

    public void updateSpecimenBarcode(Specimen specimen) {
        specimen.setBarcode(specimen.getId().toString());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            String notifCfgJson = configSvc.getFileContent(MODULE, CONFIG);
            config = parseConfig(notifCfgJson);
        } catch (Exception e) {
            logger.error("Error parsing the MCRI GenV notifications setting", e);
        }

        configSvc.registerChangeListener(MODULE,
                (name, value) -> {
                    if (!CONFIG.equals(name)) {
                        return;
                    }

                    config = parseConfig(configSvc.getFileContent(MODULE, CONFIG));
                });
    }

    private Config parseConfig(String cfgJson) {
        try {
            if (StringUtils.isBlank(cfgJson)) {
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(cfgJson, Config.class);
        } catch (Exception e) {
            logger.error("Error parsing the configuration file contents", e);
            throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, e.getMessage());
        }
    }
}
