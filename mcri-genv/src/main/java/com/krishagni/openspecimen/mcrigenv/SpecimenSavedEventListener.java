package com.krishagni.openspecimen.mcrigenv;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.de.domain.DeObject;

public class SpecimenSavedEventListener implements ApplicationListener<SpecimenSavedEvent>, InitializingBean {
	private static final Log logger = LogFactory.getLog(SpecimenSavedEventListener.class);

	private static final String MODULE = "biospecimen";

	private static final String CONFIG = "mcri_genv_notif";

	private ConfigurationService configSvc;

	private NotifCfg notifCfg;

	public void setConfigSvc(ConfigurationService configSvc) {
		this.configSvc = configSvc;
	}

	@Override
    public void onApplicationEvent(SpecimenSavedEvent event) {
		if (notifCfg == null) {
			logger.warn("Notifications setting is not configured yet. Nothing to do.");
			return;
		}

		Specimen specimen = event.getEventData();
		new MessagePublisher().publish(notifCfg.getJmsConnectionFactory(), notifCfg.getJmsNotifQueue(), specimen);
		notifyUnacceptableRecvQualities(specimen);
		notifyMissingSpecimen(specimen);
    }

	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			String notifCfgJson = configSvc.getFileContent(MODULE, CONFIG);
			notifCfg = parseConfig(notifCfgJson);
		} catch (Exception e) {
			logger.error("Error parsing the MCRI GenV notifications setting", e);
		}

		configSvc.registerChangeListener(MODULE,
			(name, value) -> {
				if (!CONFIG.equals(name)) {
					return;
				}

				notifCfg = parseConfig(configSvc.getFileContent(MODULE, CONFIG));
			}) ;
	}

	private NotifCfg parseConfig(String cfgJson) {
		try {
			if (StringUtils.isBlank(cfgJson)) {
				return null;
			}

			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(cfgJson, NotifCfg.class);
		} catch (Exception e) {
			logger.error("Error parsing the configuration file contents", e);
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, e.getMessage());
		}
	}

	private void notifyUnacceptableRecvQualities(Specimen specimen) {
		if (!specimen.isPrimary() || !specimen.isCollected()) {
			return;
		}

		List<String> unacceptableQualities = notifCfg.getUnacceptableRecvQualities();
		if (unacceptableQualities == null || unacceptableQualities.isEmpty()) {
			return;
		}

		List<String> rcpts = notifCfg.getRecvQualityNotifRcpts();
		if (rcpts == null || rcpts.isEmpty()) {
			return;
		}

		String receiveQuality = specimen.getReceivedEvent().getQuality().getValue();
		if (!unacceptableQualities.contains(receiveQuality)) {
			return;
		}

		notifyByEmail("specimen_quality_report", rcpts, specimen);
	}

	private void notifyMissingSpecimen(Specimen specimen) {
		if (!specimen.isCollected()) {
			return;
		}

		List<String> rcpts = notifCfg.getMissingSpecimenNotifRcpts();
		if (rcpts == null || rcpts.isEmpty()) {
			return;
		}

		String missingFlagFieldName = notifCfg.getMissingSpecimenFieldName();
		if (StringUtils.isBlank(missingFlagFieldName)) {
			return;
		}

		DeObject extension = specimen.getExtensionIfPresent();
		if (extension == null) {
			return;
		}

		DeObject.Attr missingSpmn = extension.getAttrs().stream()
			.filter(attr -> attr.getName().equals(missingFlagFieldName))
			.findFirst().orElse(null);
		if (missingSpmn == null || missingSpmn.getValue() == null) {
			return;
		}

		List<String> values = (List<String>) missingSpmn.getValue();
		String value = values.get(0);
		if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("1")) {
			notifyByEmail("specimen_missing_report", rcpts, specimen);
		}
	}

	public void notifyByEmail(String emailTmpl, List<String> rcpts, Specimen specimen) {
		Visit visit = specimen.getVisit();
		Site site = visit.getSite();
		CollectionProtocolRegistration cpr = visit.getRegistration();
		CollectionProtocol cp = cpr.getCollectionProtocol();

		Map<String, Object> props = new HashMap<>();
		props.put("$subject", new String[0]);
		props.put("site", site.getName());
		props.put("type", specimen.getSpecimenType().getValue());
		props.put("ppid", cpr.getPpid());
		props.put("cpShortTitle", cp.getShortTitle());
		props.put("id", specimen.getLabel());
		if (specimen.isPrimary() && specimen.isCollected()) {
			props.put("quality", specimen.getReceivedEvent().getQuality().getValue());
		}

		EmailUtil.getInstance().sendEmail(emailTmpl, rcpts.toArray(new String[0]), null, props);
	}
}
