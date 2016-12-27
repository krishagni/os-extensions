package com.krishagni.os.jhuepic;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.krishagni.os.jhuepic.dao.EpicLookupDao;

public class EpicParticipantLookup implements ParticipantLookupLogic, ConfigChangeListener, InitializingBean {

	private static Log logger = LogFactory.getLog(EpicParticipantLookup.class);

	private LocalDbParticipantLookupImpl osDbLookup;

	private DaoFactory daoFactory;

	private ConfigurationService cfgSvc;

	private ParticipantService participantSvc;

	private EpicLookupDao epicLookUpDao;

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

	public void setEpicLookUpDao(EpicLookupDao epicLookUpDao) {
		this.epicLookUpDao = epicLookUpDao;
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
		if (StringUtils.isBlank(detail.getEmpi()) && CollectionUtils.isEmpty(detail.getPmis())) {
			return osDbLookup.getMatchingParticipants(detail);
		}

		List<ParticipantDetail> epicMatchingList = new ArrayList<>();

		List<ParticipantDetail> localMatchingList = getLocalMatching(detail);
		if (CollectionUtils.isEmpty(localMatchingList)) {
			//If no local match found and eMPI is entered, then lookup into EPIC with eMPI
			if (StringUtils.isNotBlank(detail.getEmpi())) {
				ParticipantDetail epicParticipant = getParticipantFromEpic(detail.getEmpi());
				if (epicParticipant != null) {
					epicMatchingList.add(epicParticipant);
				}
				//If no eMPI entered then lookup into EPIC with the first MRN
			} else {
				ParticipantDetail epicParticipant = getParticipantFromEpic(detail.getPmis().iterator().next().getMrn());
				if (epicParticipant != null) {
					epicMatchingList.add(epicParticipant);
				}
			}
			//If local matching found then iterate and lookup in EPIC, if match found add in epicMatchingList
		} else {
			for (ParticipantDetail localParticipant : localMatchingList) {
				ParticipantDetail epicParticipant = getParticipantFromEpic(localParticipant.getEmpi());
				if (epicParticipant != null && epicParticipant.getEmpi().equals(localParticipant.getEmpi())) {
					ParticipantDetail result = merge(epicParticipant, localParticipant);
					epicMatchingList.add(result);
				}
			}
		}

		List<MatchedParticipant> matchedParticipants = new ArrayList<>();
		for (ParticipantDetail participantDetail : epicMatchingList) {
			matchedParticipants.add(new MatchedParticipant(participantDetail, Collections.singletonList("empi")));
		}
		return matchedParticipants;
	}

	private List<ParticipantDetail> getLocalMatching(ParticipantDetail detail) {
		//In the eMPI field user can enter eMPI or MRN
		//If eMPI entered then search local DB for patients by eMPI and MRN
		//If no eMPI entered then lookup by PMI
		if (StringUtils.isNotBlank(detail.getEmpi())) {
			return getLocalMatchingByEmpiMrn(detail);//local DAO to fetch participant by eMPI and MRN
		} else {
			return getLocalMatchingByPmi(detail);//local DAO to fetch participant by PMI where eMPI is not null
		}
	}

	private List<ParticipantDetail> getLocalMatchingByEmpiMrn(ParticipantDetail detail) {
		List<Participant> result = epicLookUpDao.getLocalMatchingByEmpiMrn(detail.getEmpi());

		if (CollectionUtils.isEmpty(result)) {
			return Collections.emptyList();
		}
		List<ParticipantDetail> localMatching = ParticipantDetail.from(result, false);

		ParticipantDetail exactEmpiMatch = localMatching.stream().filter(p -> detail.getEmpi().equals(p.getEmpi())).findFirst().orElse(null);

		return exactEmpiMatch == null ? localMatching : Arrays.asList(exactEmpiMatch);
	}

	private List<ParticipantDetail> getLocalMatchingByPmi(ParticipantDetail detail) {

		List<Participant> localMatching = epicLookUpDao.getLocalMatchingByPmi(detail.getPmis());
		if (CollectionUtils.isEmpty(localMatching)) {
			return Collections.emptyList();
		}
		return ParticipantDetail.from(localMatching, false);
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
			//Log the error and return null to show no matching found from EPIC
			logger.error("Error obtaining participant details", e);
			return null;
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
		participant.setMiddleName(epicPatient.getMiddleName());
		participant.setBirthDate(epicPatient.getDateOfBirth());
		participant.setGender(getMappedValue(PvAttributes.GENDER, epicPatient.getSex()));
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
						} else if (id.getType().equals("Enterprise Id")) {
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
