package com.krishagni.openspecimen.mcrigenv;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
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

	private SpecimenNotifDao specimenNotifDao;

	public void setConfigSvc(ConfigurationService configSvc) {
		this.configSvc = configSvc;
	}

	public void setSpecimenNotifDao(SpecimenNotifDao specimenNotifDao) {
		this.specimenNotifDao = specimenNotifDao;
	}

	@Override
    public void onApplicationEvent(SpecimenSavedEvent event) {
		if (notifCfg == null) {
			logger.warn("Notifications setting is not configured yet. Nothing to do.");
			return;
		}

		Specimen specimen = event.getEventData();
		List<String> cps = notifCfg.getCps();
		if (cps != null && !cps.isEmpty() && !cps.contains(specimen.getCpShortTitle())) {
			return;
		}

		new MessagePublisher().publish(notifCfg.getJmsConnectionFactory(), notifCfg.getJmsNotifQueue(), specimen);

		SpecimenNotif notif = getNotif(specimen);
		notifyUnacceptableRecvQualities(specimen, notif);
		notifyMissingSpecimen(specimen, notif);
		saveNotif(notif);
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

	private void notifyUnacceptableRecvQualities(Specimen specimen, SpecimenNotif notif) {
		notif.setReceiveQualityNotifTime(Calendar.getInstance().getTime());

		if (!specimen.isPrimary() || !specimen.isCollected()) {
			notif.setReceiveQuality(null);
			return;
		}

		PermissibleValue prevQuality = notif.getReceiveQuality();
		PermissibleValue newQuality = specimen.getReceivedEvent().getQuality();
		notif.setReceiveQuality(newQuality);

		List<String> unacceptableQualities = notifCfg.getUnacceptableRecvQualities();
		if (unacceptableQualities == null || unacceptableQualities.isEmpty()) {
			return;
		}

		List<String> rcpts = notifCfg.getRecvQualityNotifRcpts();
		if (rcpts == null || rcpts.isEmpty()) {
			return;
		}

		if (!unacceptableQualities.contains(newQuality.getValue()) || newQuality.equals(prevQuality)) {
			return;
		}

		notifyByEmail("specimen_quality_report", rcpts, specimen);
	}

	private void notifyMissingSpecimen(Specimen specimen, SpecimenNotif notif) {
		Boolean prevValue = notif.getMissed();
		notif.setMissedNotifTime(Calendar.getInstance().getTime());
		notif.setMissed(null);

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

		String value = missingSpmn.getValue().toString();
		Boolean newValue = value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("1");
		notif.setMissed(newValue);

		List<String> rcpts = notifCfg.getMissingSpecimenNotifRcpts();
		if (rcpts == null || rcpts.isEmpty()) {
			return;
		}

		if (newValue && !newValue.equals(prevValue)) {
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

	private SpecimenNotif getNotif(Specimen specimen) {
		SpecimenNotif notif = specimenNotifDao.getBySpecimen(specimen.getId());
		if (notif == null) {
			notif = new SpecimenNotif();
			notif.setSpecimen(specimen);
		}

		return notif;
	}

	private void saveNotif(SpecimenNotif notif) {
		specimenNotifDao.saveOrUpdate(notif);
	}
}
