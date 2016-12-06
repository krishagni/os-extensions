package com.krishagni.os.jhuepic;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedParticipant;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.matching.LocalDbParticipantLookupImpl;
import com.krishagni.catissueplus.core.biospecimen.matching.ParticipantLookupLogic;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.ParticipantService;
import com.krishagni.catissueplus.core.common.PvAttributes;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ConfigChangeListener;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

public class EpicParticipantLookup implements ParticipantLookupLogic, ConfigChangeListener, InitializingBean {

	private static Log logger = LogFactory.getLog(EpicParticipantLookup.class);

	private LocalDbParticipantLookupImpl osDbLookup;

	private DaoFactory daoFactory;

	private ConfigurationService cfgSvc;

	private ParticipantService participantSvc;

	public void setOsDbLookup(LocalDbParticipantLookupImpl osDbLookup) {
		this.osDbLookup = osDbLookup;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	public void setParticipantSvc(ParticipantService participantSvc) {
		this.participantSvc = participantSvc;
	}

	@Override
	public void onConfigChange(String name, String value) {
		if (!name.equals("two_step_patient_reg") || !Boolean.valueOf(value)) {
			return;
		}

		if (StringUtils.isAnyBlank(getApiUrl(), getClientId(), getClientKey())) {
			throw OpenSpecimenException.userError(EpicErrorCode.API_DETAILS_EMPTY);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		cfgSvc.registerChangeListener(ConfigParams.MODULE, this);
	}

	@Override
	public List<MatchedParticipant> getMatchingParticipants(ParticipantDetail detail) {
		if (StringUtils.isBlank(detail.getEmpi())) {
			return osDbLookup.getMatchingParticipants(detail);
		}

		ParticipantDetail epicParticipant = getParticipantFromEpic(detail.getEmpi());
		if (epicParticipant == null) {
			return Collections.emptyList();
		}

		ParticipantDetail localParticipant = getLocalParticipant(epicParticipant);
		ParticipantDetail result = merge(epicParticipant, localParticipant);
		return Collections.singletonList(new MatchedParticipant(result, Collections.singletonList("empi")));
	}

	private ParticipantDetail getParticipantFromEpic(String empi) {
		String baseUrl = ConfigUtil.getInstance().getStrSetting(JHU_EPIC_MODULE, "epic_base_url", "");

		RestTemplate template = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.set("Accept-Encoding", "gzip,deflate");
		headers.set("client_id", getClientId());
		headers.set("client_secret", getClientKey());

		HttpEntity<?> entity = new HttpEntity<>(headers);

		ResponseEntity<Map[]> result = null;
		try {
			result = template.exchange(baseUrl + empi, HttpMethod.GET, entity, Map[].class);
		} catch (Exception e) {
			logger.error("Error obtaining participant details", e);
			throw OpenSpecimenException.userError(EpicErrorCode.API_CALL_FAILED, e.getMessage());
		}

		if (!result.getStatusCode().equals(HttpStatus.OK)) {
			return null;
		}

		return getEpicParticipantDetail(result.getBody(), empi);
	}

	private ParticipantDetail getEpicParticipantDetail(Map[] result, String empi) {
		if (result == null || result.length == 0) {
			return null;
		}

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.PASCAL_CASE_TO_CAMEL_CASE);
		EpicPatient epicPatient = mapper.convertValue(result[0], EpicPatient.class);

		ParticipantDetail participant = new ParticipantDetail();
		participant.setFirstName(epicPatient.getFirstName());
		participant.setLastName(epicPatient.getLastName());
		participant.setBirthDate(epicPatient.getDateOfBirth());
		participant.setGender(getMappedValue(PvAttributes.GENDER, epicPatient.getSex()));
		participant.setVitalStatus(getMappedValue(PvAttributes.VITAL_STATUS, epicPatient.getStatus()));
		participant.setEthnicity(getMappedValue(PvAttributes.ETHNICITY, epicPatient.getEthnicGroup()));
		participant.setEmpi(empi);
		participant.setSource("EPIC");

		if (epicPatient.getRace() != null && epicPatient.getRace().length > 0) {
			participant.setRaces(Stream.of(epicPatient.getRace())
				.map(race -> getMappedValue(PvAttributes.RACE, race))
				.collect(Collectors.toSet()));
		}

		if (CollectionUtils.isNotEmpty(epicPatient.getIds())) {
			participant.setPmis(epicPatient.getIds().stream()
					.map(id -> {
						PmiDetail pmi = new PmiDetail();
						pmi.setMrn(id.getId());

						Site site = daoFactory.getSiteDao().getSiteByCode(id.getType());
						if (site == null) {
							throw OpenSpecimenException.userError(EpicErrorCode.MATCHING_SITE_NOT_FOUND, id.getType());
						}

						pmi.setSiteName(site.getName());
						return pmi;
					}).collect(Collectors.toList()));
		}

		return participant;
	}

	private ParticipantDetail getLocalParticipant(ParticipantDetail detail) {
		MatchedParticipant localMatch = osDbLookup.getMatchingParticipants(detail).stream()
			.filter(lm -> lm.getMatchedAttrs().contains("empi"))
			.findFirst().orElse(null);
		return localMatch != null ? localMatch.getParticipant() : null;
	}

	private ParticipantDetail merge(ParticipantDetail epicParticipant, ParticipantDetail localParticipant) {
		if (localParticipant == null) {
			return epicParticipant;
		}

//		BeanUtils.copyProperties(epicParticipant, localParticipant, new String[] {"id", "middleName", "sexGenotype", "uid", "activityStatus", "deathDate", "cprs"});

		epicParticipant.setId(localParticipant.getId());
		ResponseEvent<ParticipantDetail> response = participantSvc.patchParticipant(new RequestEvent<>(epicParticipant));
		response.throwErrorIfUnsuccessful();
		return response.getPayload();
	}

	private String getMappedValue(String attribute, String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}

		List<PermissibleValue> pvs = daoFactory.getPermissibleValueDao().getByPropertyKeyValue(attribute, EPIC_VALUE, value);
		if (CollectionUtils.isEmpty(pvs)) {
			throw OpenSpecimenException.userError(EpicErrorCode.PV_NOT_MAPPED, attribute, value);
		}

		return pvs.iterator().next().getValue();
	}

	private String getApiUrl() {
		return ConfigUtil.getInstance().getStrSetting(JHU_EPIC_MODULE, API_URL, "");
	}

	private String getClientId() {
		return ConfigUtil.getInstance().getStrSetting(JHU_EPIC_MODULE, CLIENT_ID, "");
	}

	private String getClientKey() {
		return ConfigUtil.getInstance().getStrSetting(JHU_EPIC_MODULE, CLIENT_KEY, "");
	}


	private static final String JHU_EPIC_MODULE = "plugin_jhu_epic";

	private static final String API_URL = "epic_base_url";

	private static final String CLIENT_ID = "epic_client_id";

	private static final String CLIENT_KEY = "epic_client_secret";

	private static final String EPIC_VALUE = "epic_value";

}
