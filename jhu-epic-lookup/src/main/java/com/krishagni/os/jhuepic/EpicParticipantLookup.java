package com.krishagni.os.jhuepic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
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
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.os.jhuepic.dao.ParticipantLookupDao;

public class EpicParticipantLookup implements ParticipantLookupLogic, ConfigChangeListener, InitializingBean {
	private static final LogUtil logger = LogUtil.getLogger(EpicParticipantLookup.class);

	private static final String MERGE_OP = "JHU-EPIC-MERGE";

	private LocalDbParticipantLookupImpl osDbLookup;

	private DaoFactory daoFactory;

	private ConfigurationService cfgSvc;

	private ParticipantService participantSvc;

	private ParticipantLookupDao participantLookUpDao;

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

	public void setParticipantLookUpDao(ParticipantLookupDao participantLookUpDao) {
		this.participantLookUpDao = participantLookUpDao;
	}

	@Override
	public void onConfigChange(String name, String value) {
		if (!name.equals("two_step_patient_reg") || !Boolean.parseBoolean(value)) {
			return;
		}

		if (StringUtils.isAnyBlank(getApiUrl(), getClientId(), getClientKey())) {
			throw OpenSpecimenException.userError(EpicErrorCode.API_DETAILS_EMPTY);
		}
	}

	@Override
	public void afterPropertiesSet() {
		cfgSvc.registerChangeListener(ConfigParams.MODULE, this);
	}

	@Override
	public List<MatchedParticipant> getMatchingParticipants(ParticipantDetail detail) {
		if (StringUtils.equals(MERGE_OP, detail.getOpComments())) {
			return Collections.singletonList(new MatchedParticipant(detail, Collections.singletonList("pmi")));
		}

		if (StringUtils.isBlank(detail.getEmpi()) && CollectionUtils.isEmpty(detail.getPmis())) {
			return osDbLookup.getMatchingParticipants(detail);
		}

		//
		// Retrieve list of participants from local OS database that match either eMPI or MRN
		//
		List<ParticipantDetail> localParticipants = getLocalParticipants(detail);

		List<ParticipantDetail> epicMatchingList = new ArrayList<>();
		if (CollectionUtils.isEmpty(localParticipants)) {
			ParticipantDetail epicParticipant;
			if (StringUtils.isNotBlank(detail.getEmpi())) {
				//
				// If no local match found and eMPI is entered, then lookup into EPIC with eMPI
				//
				epicParticipant = getParticipantFromEpic(detail.getEmpi());
			} else {
				//
				// If no eMPI entered then lookup into EPIC with the first MRN
				//
				epicParticipant = getParticipantFromEpic(detail.getPmis().iterator().next().getMrn());
			}

			if (epicParticipant != null) {
				epicMatchingList.add(epicParticipant);
			}
		} else {
			//
			// Iterate and lookup for info for each local match in EPIC
			//
			for (ParticipantDetail localParticipant : localParticipants) {
				ParticipantDetail epicParticipant = getParticipantFromEpic(localParticipant.getEmpi());
				if (epicParticipant != null && StringUtils.equals(epicParticipant.getEmpi(), localParticipant.getEmpi())) {
					ParticipantDetail result = merge(epicParticipant, localParticipant);
					epicMatchingList.add(result);
				}
			}
		}

		return epicMatchingList.stream()
			.map(participant -> {
				if (participant.getRegisteredCps() == null) {
					participant.setRegisteredCps(Collections.emptySet());
				}

				return new MatchedParticipant(participant, Collections.singletonList("pmi"));
			})
			.collect(Collectors.toList());
	}

	private List<ParticipantDetail> getLocalParticipants(ParticipantDetail detail) {
		if (StringUtils.isNotBlank(detail.getEmpi())) {
			//
			// The search text inputted by user could refer to either eMPI or MRN;
			// therefore search for participants whose eMPI or MRN matches
			// input search text
			//
			return getParticipantsByEmpiMrn(detail);
		} else {
			return getParticipantsByPmi(detail);
		}
	}

	private List<ParticipantDetail> getParticipantsByEmpiMrn(ParticipantDetail detail) {
		List<Participant> participants = participantLookUpDao.getByEmpiMrn(detail.getEmpi());
		if (CollectionUtils.isEmpty(participants)) {
			return Collections.emptyList();
		}

		Participant exactMatch = participants.stream()
			.filter(p -> detail.getEmpi().equals(p.getEmpi()))
			.findFirst().orElse(null);
		if (exactMatch != null) {
			return Collections.singletonList(ParticipantDetail.from(exactMatch, false));
		}

		return ParticipantDetail.from(participants, false);
	}

	private List<ParticipantDetail> getParticipantsByPmi(ParticipantDetail detail) {
		List<Participant> participants = participantLookUpDao.getByPmi(detail.getPmis());
		if (CollectionUtils.isEmpty(participants)) {
			return Collections.emptyList();
		}

		return ParticipantDetail.from(participants, false);
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
		} catch (HttpStatusCodeException apiError) {
			throw OpenSpecimenException.userError(EpicErrorCode.API_CALL_FAILED, getErrorMsg(apiError));
		} catch (Exception e) {
			// Log the error and return null to show no matching found from EPIC
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
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);
		mapper.setTimeZone(TimeZone.getDefault());
		EpicPatient epicPatient = mapper.convertValue(result[0], EpicPatient.class);

		ParticipantDetail participant = new ParticipantDetail();
		participant.setFirstName(epicPatient.getFirstName());
		participant.setLastName(epicPatient.getLastName());
		participant.setMiddleName(epicPatient.getMiddleName());
		participant.setBirthDate(epicPatient.getDateOfBirth());
		participant.setGender(getMappedValue(PvAttributes.GENDER, epicPatient.getSex()));
		//participant.setEmpi(empi);
		participant.setSource("EPIC");

		String ethnicity = getMappedValue(PvAttributes.ETHNICITY, epicPatient.getEthnicGroup());
		if (StringUtils.isNotBlank(ethnicity)) {
			participant.setEthnicities(Collections.singleton(ethnicity));
		}


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

					if (StringUtils.equals(site.getName(), "Enterprise ID")) {
						participant.setEmpi(pmi.getMrn());
					}

					pmi.setSiteName(site.getName());
					return pmi;
				}).collect(Collectors.toList()));
		}

		return participant;
	}

	private ParticipantDetail merge(ParticipantDetail epicParticipant, ParticipantDetail localParticipant) {
		if (localParticipant == null) {
			return epicParticipant;
		}

		epicParticipant.setId(localParticipant.getId());
		epicParticipant.setOpComments(MERGE_OP); // to indicate that this is merge and no matching should be done
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

	private String getErrorMsg(HttpStatusCodeException error) {
		String respBody = error.getResponseBodyAsString();
		if (StringUtils.isBlank(respBody)) {
			return error.getMessage();
		} else {
			try {
				Map<String, String> respMap = new ObjectMapper().readValue(respBody, Map.class);
				return respMap.get("message");
			} catch (Exception e) {
				return error.getMessage();
			}
		}
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
