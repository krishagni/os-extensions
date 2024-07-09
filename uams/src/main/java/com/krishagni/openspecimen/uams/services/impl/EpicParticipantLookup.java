package com.krishagni.openspecimen.uams.services.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedParticipant;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.matching.LocalDbParticipantLookupImpl;
import com.krishagni.catissueplus.core.biospecimen.matching.ParticipantLookupLogic;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.ParticipantService;
import com.krishagni.catissueplus.core.common.PvAttributes;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.openspecimen.uams.EpicErrorCode;
import com.krishagni.openspecimen.uams.events.EpicLookupResult;
import com.krishagni.openspecimen.uams.events.EpicPatient;

public class EpicParticipantLookup implements ParticipantLookupLogic {

	private static final LogUtil logger = LogUtil.getLogger(EpicParticipantLookup.class);

	private static final String MERGE_OP = "uams-epic-merge";

	private static final String EPIC_MODULE = "uams_epic_module";

	private static final String EPIC_VALUE = "epic_value";

	private LocalDbParticipantLookupImpl osDbLookup;

	private DaoFactory daoFactory;

	private ParticipantService participantSvc;

	public void setOsDbLookup(LocalDbParticipantLookupImpl osDbLookup) {
		this.osDbLookup = osDbLookup;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setParticipantSvc(ParticipantService participantSvc) {
		this.participantSvc = participantSvc;
	}

	@Override
	public List<MatchedParticipant> getMatchingParticipants(ParticipantDetail input) {
		if (StringUtils.equals(MERGE_OP, input.getOpComments())) {
			return Collections.singletonList(new MatchedParticipant(input, Collections.singletonList("empi")));
		}

		if (StringUtils.isBlank(input.getEmpi())) {
			return osDbLookup.getMatchingParticipants(input);
		}

		if (StringUtils.isAnyBlank(getApiUrl(), getApiKey())) {
			throw OpenSpecimenException.userError(EpicErrorCode.API_DETAILS_EMPTY);
		}

		ParticipantDetail localParticipant = getLocalParticipant(input.getEmpi());
		ParticipantDetail epicParticipant  = getEpicParticipant(input.getEmpi());
		epicParticipant = merge(epicParticipant, localParticipant);
		if (epicParticipant == null) {
			return Collections.emptyList();
		} else if (epicParticipant.getRegisteredCps() == null) {
			epicParticipant.setRegisteredCps(Collections.emptySet());
		}

		return Arrays.asList(new MatchedParticipant(epicParticipant, Collections.singletonList("empi")));
	}

	private ParticipantDetail getLocalParticipant(String empi) {
		Participant participant = daoFactory.getParticipantDao().getByEmpi(empi);
		return participant == null ? null : ParticipantDetail.from(participant, false);
	}

	private ParticipantDetail getEpicParticipant(String empi) {
		RestTemplate template = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.set("Accept-Encoding", "gzip,deflate");
		headers.set("key", getApiKey());

		HttpEntity<?> entity = new HttpEntity<>(headers);

		ResponseEntity<Map> result = null;
		try {
			Map<String, String> query = Collections.singletonMap("mrn", empi);
			result = template.exchange(getApiUrl() + "?mrn={mrn}", HttpMethod.GET, entity, Map.class, query);
		} catch (HttpStatusCodeException apiError) {
			throw OpenSpecimenException.userError(EpicErrorCode.API_CALL_FAILED, empi, getErrorMsg(apiError));
		} catch (Exception e) {
			// Log the error and return null to show no matching found from EPIC
			logger.error("Error obtaining participant details", e);
			throw OpenSpecimenException.userError(EpicErrorCode.API_CALL_FAILED, empi, e.getMessage());
		}

		if (!result.getStatusCode().equals(HttpStatus.OK)) {
			return null;
		}

		return getEpicParticipantDetail(empi, result.getBody());
	}

	private String getApiUrl() {
		return ConfigUtil.getInstance().getStrSetting(EPIC_MODULE, "epic_api_url", "");
	}

	private String getApiKey() {
		return ConfigUtil.getInstance().getStrSetting(EPIC_MODULE, "epic_api_key", "");
	}

	private String getErrorMsg(HttpStatusCodeException error) {
		String respBody = error.getResponseBodyAsString();
		logger.error("Received error when performing EPIC participant lookups: " + respBody);
		if (StringUtils.isBlank(respBody)) {
			return error.getMessage();
		} else {
			try {
				Map<String, Object> respMap = Utility.jsonToMap(respBody);
				String msg = "";
				if (respMap.get("status") != null) {
					msg += respMap.get("status");
				}

				if (respMap.get("error") != null) {
					if (!msg.isEmpty()) {
						msg += ": ";
					}

					msg += respMap.get("error");
				}

				return msg.isEmpty() ? respBody : msg;
			} catch (Exception e) {
				return respBody;
			}
		}
	}

	private ParticipantDetail getEpicParticipantDetail(String empi, Map result) {
		if (result == null || result.isEmpty()) {
			return null;
		}

		EpicLookupResult lookupResult = Utility.mapToObject(result, EpicLookupResult.class);
		if (StringUtils.isNotBlank(lookupResult.getError())) {
			throw OpenSpecimenException.userError(EpicErrorCode.API_CALL_FAILED, empi, lookupResult.getError());
		}

		if (!(lookupResult.getResults() instanceof Map)) {
			// throw OpenSpecimenException.userError(EpicErrorCode.API_CALL_FAILED, empi, "No matching participant.");
			return null;
		}

		EpicPatient epicPatient = Utility.mapToObject((Map<String, Object>) lookupResult.getResults(), EpicPatient.class);

		ParticipantDetail participant = new ParticipantDetail();
		participant.setSource("EPIC");
		participant.setFirstName(epicPatient.getFirstName());
		participant.setLastName(epicPatient.getLastName());
		participant.setMiddleName(epicPatient.getMiddleName());
		participant.setBirthDateStr(epicPatient.getBirthDate());
		participant.setEmpi(epicPatient.getMrn());
		if (StringUtils.isNotBlank(epicPatient.getBirthDate())) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				sdf.setTimeZone(TimeZone.getDefault());
				participant.setBirthDate(sdf.parse(epicPatient.getBirthDate()));
			} catch (ParseException pe) {
				throw OpenSpecimenException.userError(EpicErrorCode.API_CALL_FAILED, empi, "Parsing birth date " + epicPatient.getBirthDate() + " failed with error: " + pe.getMessage());
			}
		}

		participant.setGender(getMappedValue(PvAttributes.GENDER, epicPatient.getGenderCode()));

		String race = getMappedValue(PvAttributes.RACE, epicPatient.getRaceCode());
		if (StringUtils.isNotBlank(race)) {
			participant.setRaces(Collections.singleton(race));
		}

		String ethnicity = getMappedValue(PvAttributes.ETHNICITY, epicPatient.getEthnicCode());
		if (StringUtils.isNotBlank(ethnicity)) {
			participant.setEthnicities(Collections.singleton(ethnicity));
		}

		return participant;
	}

	private String getMappedValue(String attribute, String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}

		List<PermissibleValue> pvs = daoFactory.getPermissibleValueDao().getByPropertyKeyValue(attribute, EPIC_VALUE, value);
		if (pvs == null || pvs.isEmpty()) {
			throw OpenSpecimenException.userError(EpicErrorCode.PV_NOT_MAPPED, attribute, value);
		}

		return pvs.iterator().next().getValue();
	}

	private ParticipantDetail merge(ParticipantDetail epicParticipant, ParticipantDetail localParticipant) {
		if (localParticipant == null || epicParticipant == null) {
			return epicParticipant;
		}

		epicParticipant.setId(localParticipant.getId());
		epicParticipant.setOpComments(MERGE_OP); // to indicate that this is merge and no matching should be done
		return ResponseEvent.unwrap(participantSvc.patchParticipant(RequestEvent.wrap(epicParticipant)));
	}
}
