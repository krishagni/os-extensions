package com.krishagni.openspecimen.redcap;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import krishagni.catissueplus.beans.FormContextBean;
import krishagni.catissueplus.beans.FormRecordEntryBean;
import krishagni.catissueplus.beans.FormRecordEntryBean.Status;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.biospecimen.services.VisitService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.de.events.FormType;
import com.krishagni.catissueplus.core.de.repository.FormDao;
import com.krishagni.openspecimen.redcap.crf.Instrument;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.DatePicker;
import edu.common.dynamicextensions.domain.nui.PermissibleValue;
import edu.common.dynamicextensions.domain.nui.SelectControl;
import edu.common.dynamicextensions.domain.nui.UserContext;
import edu.common.dynamicextensions.napi.ControlValue;
import edu.common.dynamicextensions.napi.FormData;
import edu.common.dynamicextensions.napi.FormDataManager;

@Configurable
public class Project extends BaseEntity {
	private static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
	
	private static SimpleDateFormat fieldDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	
	private static String AUDIT_API = "plugins/open/log_api.php";
	
	private String name;

	private String hostUrl;
		
	private Long projectId;
	
	private String apiToken;

	private List<String> transformerFqns = new ArrayList<String>();
	
	private List<FieldTransformer> transformers = new ArrayList<FieldTransformer>();
	
	private Map<String, String>	subjectFields = new HashMap<String, String>();
	
	private Map<String, String> visitFields = new HashMap<String, String>();
	
	private CollectionProtocol collectionProtocol;
	
	private User updatedBy;
	
	private Date updateTime;
	
	private String activityStatus;
		
	private Map<String, Control> dictionary;
	
	@Autowired
	private DaoFactory daoFactory;
	
	@Autowired
	private FormDao formDao;
	
	@Autowired
	private RecordDao recordDao;
	
	@Autowired
	private CollectionProtocolRegistrationService cprSvc;
	
	@Autowired
	private VisitService visitSvc;
	
	@Autowired
	private FormDataManager formDataMgr;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHostUrl() {
		return hostUrl;
	}

	public void setHostUrl(String hostUrl) {
		this.hostUrl = hostUrl;
	}
	
	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public String getApiToken() {
		return apiToken;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}
	
	public String getCpShortTitle() {
		return collectionProtocol.getShortTitle();
	}

	public String getAuditApiUrl() {
		StringBuilder result = new StringBuilder();
		
		result.append(getHostUrl());
		if (!getHostUrl().endsWith("/")) {
			result.append("/");
		}
		
		result.append(AUDIT_API);
		return result.toString();
	}
	
	public String getApiUrl() {
		StringBuilder result = new StringBuilder();
		
		result.append(getHostUrl());
		if (!getHostUrl().endsWith("/")) {
			result.append("/");
		}

		result.append("api/");
		return result.toString();
	}

	public List<String> getTransformerFqns() {
		return transformerFqns;
	}

	public void setTransformerFqns(List<String> transformerFqns) {
		this.transformerFqns = transformerFqns;		
		if (transformerFqns == null) {
			return;
		}
		
		transformers.clear();
		try {
			for (String classFqn : transformerFqns) {
				transformers.add((FieldTransformer)Class.forName(classFqn).newInstance());				
			}			
		} catch (Exception e) {
			throw new RuntimeException("Error initialising field transformers");
		}		
	}
	
	public List<FieldTransformer> getTransformers() {
		return transformers;
	}
	
	public void setTransformersJson(String transformersJson) {
		List<String> fqns = jsonToObject(transformersJson);
		setTransformerFqns(fqns);
	}
	
	public String getTransformersJson() {
		return objectToJson(transformerFqns);
	}
	
	public Map<String, String> getSubjectFields() {
		return subjectFields;
	}

	public void setSubjectFields(Map<String, String> subjectFields) {
		this.subjectFields = subjectFields;
	}
	
	public String getSubjectFieldsJson() {
		return objectToJson(subjectFields);
	}
	
	public void setSubjectFieldsJson(String subjectFieldsJson) {
		this.subjectFields = jsonToObject(subjectFieldsJson);
	}

	public Map<String, String> getVisitFields() {
		return visitFields;
	}

	public void setVisitFields(Map<String, String> visitFields) {
		this.visitFields = visitFields;
	}

	public String getVisitFieldsJson() {
		return objectToJson(visitFields);
	}
	
	public void setVisitFieldsJson(String visitFieldsJson) {
		this.visitFields = jsonToObject(visitFieldsJson);
	}
	
	public CollectionProtocol getCollectionProtocol() {
		return collectionProtocol;
	}
	
	public void setCollectionProtocol(CollectionProtocol collectionProtocol) {
		this.collectionProtocol = collectionProtocol;
	}
	
	public User getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(User updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public void update(Project other) {
		setName(other.getName());
		setHostUrl(other.getHostUrl());
		setProjectId(other.getProjectId());
		
		if (StringUtils.isNotBlank(other.getApiToken())) {
			setApiToken(other.getApiToken());
		}
		
		setTransformerFqns(other.getTransformerFqns());
		setSubjectFields(other.getSubjectFields());
		setVisitFields(other.getVisitFields());
		setCollectionProtocol(other.getCollectionProtocol());
		setUpdatedBy(other.getUpdatedBy());
		setUpdateTime(other.getUpdateTime());		
		setActivityStatus(other.getActivityStatus());
	}
	
	public void updateInstruments() {
		updateInstruments(null);
	}
	
	public Set<Instrument> updateInstruments(Set<String> instrumentNames) {		
		Set<Instrument> instruments = new LinkedHashSet<Instrument>();
		if (CollectionUtils.isEmpty(instrumentNames)) {
			instruments.addAll(getInstruments());
		} else {
			for (Instrument instrument : getInstruments()) {
				if (instrumentNames.contains(instrument.getName())) {
					instruments.add(instrument);
				}
			}
		}
		
		CollectionProtocol cp = getCollectionProtocol();
		for (Instrument instrument : instruments) {
			instrument.loadInstrument();			
			saveOrUpdateForm(cp, instrument);			
		}
		
		return instruments;
	}
	
	@SuppressWarnings("unchecked")
	public List<LogEvent> getEvents(Date startTs, Date endTs) {		
		MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
		form.add("token", getApiToken());
		form.add("format", "json");
		form.add("endts", formatter.format(endTs));
		if (startTs != null) {
			form.add("startts", formatter.format(startTs));
		}
		
		System.err.println("RC: Making call to RC...");		
		RestTemplate client = new RestTemplate();
		Map<String, String>[] eventsMap = client.postForObject(getAuditApiUrl(), form, Map[].class);
		
		List<LogEvent> events = new ArrayList<LogEvent>();
		for (Map<String, String> eventMap : eventsMap) {
			String objectType = eventMap.get("object_type");
			if (!"redcap_data".equals(objectType)) {
				continue;
			}
			
			try {
				LogEvent event = LogEvent.parse(eventMap);
				switch (event.getType()) {
					case INSERT:
					case UPDATE:
						System.err.println("Adding event: \n" + event);
						events.add(0, event);
						break;
					
					default:
						System.err.println("Ignoring event: \n" + event);						
				}
			} catch (Exception e) {
				System.err.println("Could not parse");
				System.err.println(eventMap);
				System.err.println("***");
			}
			
		}
		
		System.err.println("RC: Obtained events: " + events.size());
		return events;		
	}
	
	@PlusTransactional
	public void processEvent(LogEvent event) {
		for (FieldTransformer transformer : getTransformers()) {
			event = transformer.transform(event);
		}
		
		Record record = recordDao.getByRecordId(getId(), event.getPk());
		if (record == null) {
			record = new Record();
			record.setRecordId(event.getPk());
			record.setProject(this);
		}
		
		CollectionProtocol cp = getCollectionProtocol();		
		Long cprId = saveOrUpdateSubject(cp, record, event);
		if (cprId != null && record.getCprId() == null) {
			record.setCprId(cprId);
		}
		
	    Long visitId = saveOrUpdateVisit(cp, record, event);
	    if (visitId != null && record.getVisitId() == null) {
	    	record.setVisitId(visitId);
	    }
	    
	    saveOrUpdateFormData(cp, record, event);
	    
	    if (record.getCprId() == null) {
	    	throw OpenSpecimenException.userError(ProjectErrorCode.CPR_NOT_FOUND, event.getPk());
	    } else if (record.getVisitId() == null) {
	    	throw OpenSpecimenException.userError(ProjectErrorCode.VISIT_NOT_FOUND, event.getPk());
	    } else {
	    	recordDao.saveOrUpdate(record);
	    }
	}
	
	@SuppressWarnings("unchecked")
	public List<Instrument> getInstruments() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
		form.add("token", getApiToken());
		form.add("content", "instrument");
		form.add("format", "json");
		
		return getInstruments(new RestTemplate().postForObject(getApiUrl(), form, Map[].class));
	}
	
	//
	// TODO: what will happen if there are other visit forms?
	//
	@PlusTransactional
	public void loadDictionary() {
		if (this.dictionary != null && !this.dictionary.isEmpty()) {
			return;
		}
		
		CollectionProtocol cp = getCollectionProtocol();
		List<Long> formIds = formDao.getFormIds(cp.getId(), FormType.VISIT_FORMS.getType());
		
		Map<String, Control> dictionary = new HashMap<String, Control>();
		for (Long formId : formIds) {
			Container form = Container.getContainer(formId);
			for (Control control : form.getAllControls()) {
				dictionary.put(control.getUserDefinedName(), control);
			}
		}
		
		this.dictionary = dictionary;
	}
	
	public void unloadDictionary() {
		if (dictionary == null || dictionary.isEmpty()) {
			return;
		}
		
		dictionary.clear();
		dictionary = null;		
	}
	
	public static List<Project> parseProjectFieldsMappers(String mappingFilePath) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(new File(mappingFilePath), new TypeReference<List<Project>>() {});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}	
	}
	
	private List<Instrument> getInstruments(Map<String, String>[] data) {
		List<Instrument> instruments = new ArrayList<Instrument>();
		for (Map<String, String> instrumentMap : data) {
			Instrument instrument = new Instrument();
			instrument.setName(instrumentMap.get("instrument_name"));
			instrument.setCaption(instrumentMap.get("instrument_label"));
			instrument.setProject(this);
			
			instruments.add(instrument);
		}
		
		return instruments;		
	}
	
	private void saveOrUpdateForm(CollectionProtocol cp, Instrument instrument) {
		Long formId = Container.createContainer(getUserContext(), instrument.getDeForm(), true);
		saveOrUpdateFormCtxt(formId, cp.getId());		
	}
	
	private void saveOrUpdateFormCtxt(Long formId, Long cpId) {
		FormContextBean formCtx = formDao.getFormContext(formId, cpId, FormType.VISIT_FORMS.getType());		
		if (formCtx != null) {
			return;
		}
		
		formCtx = new FormContextBean();
		formCtx.setContainerId(formId);
		formCtx.setCpId(cpId);
		formCtx.setEntityType(FormType.VISIT_FORMS.getType());
		formDao.saveOrUpdate(formCtx);
	}
	
	private Long saveOrUpdateSubject(CollectionProtocol cp, Record record, LogEvent event) {
		String subjectFormName = subjectFields.get("$form");
		
		boolean isSubjectEvent = false;
		for (Map.Entry<String, String> field : event.getDataValues().entrySet()) {
			if (isFormField(subjectFormName, field.getKey())) {
				isSubjectEvent = true;
				break;
			}	
		}
		
		if (!isSubjectEvent) {
			return null;
		}
		
		Map<String, Object>	fields = new HashMap<String, Object>();
		fields.putAll(getFields(getSubjectFields(), event.getDataValues()));		
		fields.put("id", record.getCprId());
		fields.put("cpId", cp.getId());
				
		if (!fields.containsKey("registrationDate")) {
			fields.put("registrationDate", event.getTs());
		}
		
		return saveOrUpdateSubject(getObject(fields, CollectionProtocolRegistrationDetail.class));		
	}
		
	private Long saveOrUpdateSubject(CollectionProtocolRegistrationDetail cpr) {
		Long cprId = cpr.getId();
		if (cprId == null && StringUtils.isNotBlank(cpr.getPpid())) {
			CollectionProtocolRegistration existing = daoFactory.getCprDao().getCprByPpid(cpr.getCpId(), cpr.getPpid());			
			if (existing != null) {
				cprId = existing.getId();
				cpr.setId(cprId);
			}
		}
		
		if (cpr.getParticipant() == null) {
			cpr.setParticipant(new ParticipantDetail());
		}

		RequestEvent<CollectionProtocolRegistrationDetail> req = new RequestEvent<CollectionProtocolRegistrationDetail>(cpr);
		ResponseEvent<CollectionProtocolRegistrationDetail> resp = null;
		if (cprId == null) {			
			resp = cprSvc.createRegistration(req);
		} else {
			resp = cprSvc.updateRegistration(req);
		}
		
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload().getId();		
	}

	private Long saveOrUpdateVisit(CollectionProtocol cp, Record record, LogEvent event) {
		String visitFormName = visitFields.get("$form");
		
		boolean isVisitEvent = false;		
		for (Map.Entry<String, String> field : event.getDataValues().entrySet()) {
			if (isFormField(visitFormName, field.getKey())) {
				isVisitEvent = true;
				break;
			}	
		}
		
		if (!isVisitEvent || record.getCprId() == null) {
			return null;
		}
		
		Map<String, Object> fields = new HashMap<String, Object>();
		fields.putAll(getFields(getVisitFields(), event.getDataValues()));
		fields.put("id", record.getVisitId());
		fields.put("cprId", record.getCprId());
		fields.put("cpId", cp.getId());
		fields.put("cpShortTitle", cp.getShortTitle());
		
		
		if (!fields.containsKey("visitDate")) {
			fields.put("visitDate", event.getTs());
		}
		
		return saveOrUpdateVisit(getObject(fields, VisitDetail.class));
	}
	
	private Long saveOrUpdateVisit(VisitDetail visit) {
		Long visitId = visit.getId();
		if (visitId == null && StringUtils.isNotEmpty(visit.getName())) {
			Visit existing = daoFactory.getVisitsDao().getByName(visit.getName());
			if (existing != null) {
				visitId = existing.getId();
				visit.setId(visitId);
			}
		}
		
		if (visitId == null && StringUtils.isBlank(visit.getStatus())) {
			visit.setStatus(Visit.VISIT_STATUS_COMPLETED);
		}
		
		RequestEvent<VisitDetail> req = new RequestEvent<VisitDetail>(visit);
		ResponseEvent<VisitDetail> resp = null;		
		if (visitId != null) {
			resp = visitSvc.patchVisit(req);
		} else {
			resp = visitSvc.addVisit(req);
		}
		
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload().getId();
	}

	private boolean isFormField(String formName, String fieldName) {
		Control ctrl = dictionary.get(fieldName);
		if (ctrl == null) {
			return false;
		}
		
		Container form = ctrl.getContainer();
		return form.getName().equals(formName);		
	}
	
	private void saveOrUpdateFormData(CollectionProtocol cp, Record record, LogEvent event) {
		if (record.getVisitId() == null) {
			return;
		}
		
		Container form = getForm(event);
		if (form == null) {
			return;
		}
		
		FormContextBean formCtxt = formDao.getFormContext(form.getId(), cp.getId(), FormType.VISIT_FORMS.getType());
		if (formCtxt == null) {
			return; // throw an error
		}
		
		FormData formData = null;
		FormRecordEntryBean re = getFormRecordEntryBean(formCtxt.getIdentifier(), record.getVisitId());
		if (re == null) {
			formData = new FormData(form);
		} else {
			formData = formDataMgr.getFormData(form, re.getRecordId());
		}
		
		for (ControlValue fieldValue : getFieldValues(event)) {
			formData.addFieldValue(fieldValue);
		}
		
		Long reId = formDataMgr.saveOrUpdateFormData(getUserContext(), formData);
		if (re == null) {
			re = new FormRecordEntryBean();
			re.setFormCtxtId(formCtxt.getIdentifier());
			re.setObjectId(record.getVisitId());
			re.setRecordId(reId);
			re.setActivityStatus(Status.ACTIVE);
		}
		
		re.setUpdatedBy(1L);
		re.setUpdatedTime(Calendar.getInstance().getTime());
		formDao.saveOrUpdateRecordEntry(re);
	}
		
	private UserContext getUserContext() {
		return new UserContext() {
			@Override
			public String getIpAddress() {
				return AuthUtil.getRemoteAddr();
			}

			@Override
			public Long getUserId() {
				return AuthUtil.getCurrentUser().getId();
			}

			@Override
			public String getUserName() {				
				return AuthUtil.getCurrentUser().getLoginName();
			}			
		};
	}
	
	private Map<String, String> getFields(Map<String, String> fieldsMap, Map<String, String> values) {
		Map<String, String> data = new HashMap<String, String>();
		
		for (Map.Entry<String, String> kv : fieldsMap.entrySet()) {
			if (kv.getKey().startsWith("$")) {
				continue;
			}
			
			data.put(kv.getValue(), values.get(kv.getKey()));
		}
		
		return data;		
	}
	
//	private boolean isCompleted(LogEvent event, Map<String, String> fieldsMap) {
//		String formName = fieldsMap.get("$form");
//		Map<String, String> rcData = event.getDataValues();		
//		String status = rcData.get(formName + "_complete");
//		return COMPLETED.equals(status);
//	}
	
	private <T> T getObject(Map<String, Object> fields, Class<T> klass) {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.convertValue(fields, klass);
	}
	
	private Container getForm(LogEvent event) {
		for (Map.Entry<String, String> fieldValue : event.getDataValues().entrySet()) {
			Control ctrl = dictionary.get(fieldValue.getKey());
			if (ctrl != null) {
				return ctrl.getContainer();
			}
		}
		
		return null;
	}
	
	private FormRecordEntryBean getFormRecordEntryBean(Long formCtxtId, Long objectId) {
		List<FormRecordEntryBean> recordEntries = formDao.getRecordEntries(formCtxtId, objectId);
		return CollectionUtils.isEmpty(recordEntries) ? null : recordEntries.iterator().next();
	}
	
	private List<ControlValue> getFieldValues(LogEvent event) {
		List<ControlValue> fieldValues = new ArrayList<ControlValue>();
		
		for (Map.Entry<String, String> dv : event.getDataValues().entrySet()) {
			Control field = dictionary.get(dv.getKey());
			if (field == null) {
				continue;
			}
			
			String value = dv.getValue();
			if (field instanceof SelectControl && value != null) {
				SelectControl selectField = (SelectControl)field;
				for (PermissibleValue pv : selectField.getPvs()) {
					if (value.equals(pv.getOptionName())) {
						value = pv.getValue();
						break;
					}
				}
			} else if (field instanceof DatePicker && value != null) {
				value = parseDateFieldValue(value);
			}
			
			fieldValues.add(new ControlValue(field, value));
		}
		
		return fieldValues;
	}
	
	private String parseDateFieldValue(String value) {
		try {
			if (StringUtils.isBlank(value)) {
				return null;
			}
			
			return String.valueOf(fieldDateFormatter.parse(value).getTime());
		} catch (Exception e) {
			throw new RuntimeException("Couldn't parse date value: " + value, e);
		}
	}
	
	private String objectToJson(Object object) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			throw new RuntimeException("Error converting object to JSON", e);
		}
	}
	
	private <T> T jsonToObject(String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(json, new TypeReference<T>() {});
		} catch (Exception e) {
			throw new RuntimeException("Error converting JSON to object", e);
		}
	}
}