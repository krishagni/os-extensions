package com.krishagni.os.jhuepic;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
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
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.PvAttributes;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ConfigChangeListener;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

@Configurable
public class EpicParticipantLookup implements ParticipantLookupLogic, ConfigChangeListener, InitializingBean {

	private static Log logger = LogFactory.getLog(EpicParticipantLookup.class);
	
	@Autowired
	private LocalDbParticipantLookupImpl osParticipantLookup;
	
	@Autowired
	private DaoFactory daoFactory;
	
	@Autowired
	private ConfigurationService cfgSvc;

	@Autowired
	private ParticipantService participantSvc;

	public void setOsParticipantLookup(LocalDbParticipantLookupImpl osParticipantLookup) {
		this.osParticipantLookup = osParticipantLookup;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setParticipantSvc(ParticipantService participantSvc) {
		this.participantSvc = participantSvc;
	}

	@Override
	public void onConfigChange(String name, String value) {
		if (name.equals("two_step_patient_reg") && Boolean.valueOf(value)) {
			String baseUrl = ConfigUtil.getInstance().getStrSetting(JHU_EPIC_MODULE, "epic_base_url", "");
			String clientId = ConfigUtil.getInstance().getStrSetting(JHU_EPIC_MODULE, "epic_client_id", "");
			String clientSecret = ConfigUtil.getInstance().getStrSetting(JHU_EPIC_MODULE, "epic_client_secret", "");

			if (StringUtils.isAnyBlank(baseUrl, clientId, clientSecret)) {
				throw OpenSpecimenException.userError(EpicErrorCode.API_DETAILS_CANNOT_EMPTY);
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		cfgSvc.registerChangeListener(ConfigParams.MODULE, this);
	}

	@Override
	public List<MatchedParticipant> getMatchingParticipants(ParticipantDetail detail) {
		if (StringUtils.isBlank(detail.getEmpi())) {
			return osParticipantLookup.getMatchingParticipants(detail);
		}

		ParticipantDetail epicPart = getMatchingFromEpic(detail.getEmpi());
		if (epicPart == null) {
			return Collections.EMPTY_LIST;
		}

		List<MatchedParticipant> localMatchingList = osParticipantLookup.getMatchingParticipants(epicPart);

		ParticipantDetail localPart = localMatchingList.isEmpty() ? null : localMatchingList.iterator().next().getParticipant();

		return Collections.singletonList(new MatchedParticipant(update(epicPart, localPart), Collections.singletonList("empi")));
	}

	private ParticipantDetail getMatchingFromEpic(String empi) {
		String baseUrl = ConfigUtil.getInstance().getStrSetting(JHU_EPIC_MODULE, "epic_base_url", "");
		String clientId = ConfigUtil.getInstance().getStrSetting(JHU_EPIC_MODULE, "epic_client_id", "");
		String clientSecret = ConfigUtil.getInstance().getStrSetting(JHU_EPIC_MODULE, "epic_client_secret", "");

		RestTemplate template = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
	    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	    headers.set("Accept-Encoding", "gzip,deflate");
	    headers.set("client_id", clientId);
	    headers.set("client_secret", clientSecret);

	    HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
	    ResponseEntity<String> result = null;

	    try {
	    	result = template.exchange(baseUrl + empi, HttpMethod.GET, entity, String.class);
	    } catch (Exception e) {
	    	logger.error("No resutls found");
	    	return null;
	    }

	    if (!result.getStatusCode().equals(HttpStatus.OK)) {
	    	return null;
	    }

		return getParticipantdetails(result.getBody(), empi);
	}

	private ParticipantDetail getParticipantdetails(String response, String empi) {
		JsonElement jelement = new JsonParser().parse(response);
		JsonArray arr = jelement.getAsJsonArray();
		Iterator i = arr.iterator();
		ParticipantDetail epicPart = null;
		while (i.hasNext()) {

			epicPart = new ParticipantDetail();
			JsonObject partJson = (JsonObject) i.next();
			JsonObject name = (JsonObject) partJson.get("NameComponents");
			epicPart.setFirstName(name.get("FirstName").getAsString());
			epicPart.setLastName(name.get("LastName").getAsString());

			String birthDate = partJson.get("DateOfBirth").getAsString();
			if (StringUtils.isNotBlank(birthDate)) {
				try {
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
					epicPart.setBirthDate(formatter.parse(partJson.get("DateOfBirth").getAsString()));
				} catch (ParseException e) {
					throw OpenSpecimenException.serverError(e);
				}
			}

			epicPart.setGender(getMappedValue(PvAttributes.GENDER, EpicErrorCode.GENDER_MAPPING_NOT_FOUND, partJson.get("Sex").getAsString()));
			epicPart.setVitalStatus(getMappedValue(PvAttributes.VITAL_STATUS, EpicErrorCode.VITAL_STAT_MAPPING_NOT_FOUND, partJson.get("Status").getAsString()));
			epicPart.setEthnicity(getMappedValue(PvAttributes.ETHNICITY, EpicErrorCode.ETHNICITY_MAPPING_NOT_FOUND, partJson.get("EthnicGroup").getAsString()));
			epicPart.setEmpi(empi);

			JsonArray raceJson = partJson.get("Race").getAsJsonArray();
			Iterator epicRaceItr = raceJson.iterator();
			Set<String> races = new HashSet<>();
			while (epicRaceItr.hasNext()) {
				JsonPrimitive race = (JsonPrimitive)epicRaceItr.next();
				races.add(getMappedValue(PvAttributes.RACE, EpicErrorCode.RACE_MAPPING_NOT_FOUND, race.getAsString()));
			}

			epicPart.setRaces(races);

			List<PmiDetail> pmiList = new ArrayList<>();
			JsonArray pmisJson = partJson.get("IDs").getAsJsonArray();
			Iterator pmiItr = pmisJson.iterator();
			while (pmiItr.hasNext()) {
				PmiDetail pmiDetail = new PmiDetail();
				JsonObject pmi = (JsonObject) pmiItr.next();
				pmiDetail.setMrn(pmi.get("ID").getAsString());
				String siteName = pmi.get("Type").getAsString();
				Site site = daoFactory.getSiteDao().getSiteByCode(siteName);
				if (site == null) {
					logger.error("No site found with code: " + siteName);
					throw OpenSpecimenException.serverError(EpicErrorCode.MATHCING_SITE_NOT_FOUND, siteName);
				}
				if ("EMRN".equals(siteName)) {
					epicPart.setEmpi(pmiDetail.getMrn());
				}
				pmiDetail.setSiteName(site.getName());
				pmiList.add(pmiDetail);
			}
			epicPart.setPmis(pmiList);
			epicPart.setSource("EPIC");
		}
		return epicPart;
	}

	@PlusTransactional
	private ParticipantDetail update(ParticipantDetail epicPart, ParticipantDetail localPart) {
		if (localPart == null) {
			return epicPart;
		}

		BeanUtils.copyProperties(epicPart, localPart, new String[] {"id", "middleName", "sexGenotype", "uid", "activityStatus", "deathDate", "cprs"});
		ResponseEvent<ParticipantDetail> response = participantSvc.updateParticipant(new RequestEvent<ParticipantDetail>(localPart));
		response.throwErrorIfUnsuccessful();

		return response.getPayload();
	}

	private String getMappedValue(String attribute, EpicErrorCode errorcode, String value) {
		if (StringUtils.isBlank(value)) {
			return "";
		}
		List<PermissibleValue> pvs = daoFactory.getPermissibleValueDao().getByPropertyKeyValue(attribute, EPIC_OS_PV_MAPPING_KEY, value);
		if (CollectionUtils.isEmpty(pvs)) {
			throw OpenSpecimenException.serverError(errorcode, value);
		}

		return pvs.iterator().next().getValue();
	}

	private static final String JHU_EPIC_MODULE = "plugin_jhu_epic";

	private static final String EPIC_OS_PV_MAPPING_KEY = "epic_os_mapping";

}
