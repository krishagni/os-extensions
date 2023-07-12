package com.krishagni.openspecimen.csiro;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.context.ApplicationListener;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.CprSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.repository.FormDao;
import com.krishagni.catissueplus.core.de.services.FormService;

import edu.common.dynamicextensions.domain.nui.AbstractLookupControl;
import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.FileUploadControl;
import edu.common.dynamicextensions.domain.nui.SubFormControl;
import edu.common.dynamicextensions.napi.ControlValue;
import edu.common.dynamicextensions.napi.FileControlValue;
import edu.common.dynamicextensions.napi.FormData;

public class CprStatusHandler implements ApplicationListener<CprSavedEvent> {
	private static final String CONSENT_STATUS_PENDING = "Pending";

	private static final String CONSENT_STATUS_CLARIFY = "Clarify";

	private static final String CONSENT_STATUS_ACTIVE = "Active";

	private static final String CONSENT_STATUS_INACTIVE = "Inactive";

	private static final String CONSENT_STATUS_WITHDRAWN = "Withdrawn";

	private FormDao formDao;

	private FormService formSvc;

	public void setFormDao(FormDao formDao) {
		this.formDao = formDao;
	}

	public void setFormSvc(FormService formSvc) {
		this.formSvc = formSvc;
	}

	@Override
	public void onApplicationEvent(CprSavedEvent event) {
		CollectionProtocolRegistration cpr = event.getEventData();
		handleStatus(cpr, null);
	}

	public void handleStatus(CollectionProtocolRegistration cpr, FormData customForm) {
		Participant participant = cpr.getParticipant();
		DeObject customFields = cpr.getExtension();

		Object consentWithdrawn = customFields.getAttrValue("consent_withdrawn");
		if ("Yes".equals(consentWithdrawn)) {
			setConsentStatusWithdrawn(customFields);
			return;
		}

		if (StringUtils.isBlank(participant.getEmpi()) ||
			StringUtils.isBlank(participant.getLastName()) ||
			StringUtils.isBlank(participant.getFirstName()) ||
			participant.getBirthDate() == null) {
			setConsentStatusPending(customFields);
			return;
		}

		Object signed = getAttrValue(customFields, "participant_signed");
		if (Objects.isNull(signed) || signed.toString().isEmpty() || signed.toString().equals("No")) {
			setConsentStatusPending(customFields);
			return;
		}

		Object signDate = getAttrValue(customFields, "date_participant_signed");
		if (signDate == null || signDate.toString().isEmpty()) {
			setConsentStatusPending(customFields);
			return;
		}

		long eighteenMs = DateUtils.addYears(participant.getBirthDate(), 18).getTime();
		if (eighteenMs > Long.parseLong(signDate.toString())) {
			setConsentStatusPending(customFields);
			return;
		}

		Map<String, List<Long>> formRecordIds = formDao.getEntityFormRecordIds(
			Collections.singleton("Participant"),
			cpr.getId(),
			Collections.singleton("nhms_participant_consent_form"));
		if (formRecordIds.isEmpty()) {
			setConsentStatusPending(customFields);
			return;
		}

		if (customForm == null) {
			List<Long> recordIds = formRecordIds.get("nhms_participant_consent_form");
			if (recordIds == null || recordIds.isEmpty()) {
				setConsentStatusPending(customFields);
				return;
			}

			Container form = Container.getContainer("nhms_participant_consent_form");
			customForm = formSvc.getRecord(form, recordIds.iterator().next());
		}

		Map<String, Object> recordValues = toValueMap(customForm);
		Object genomicResearchConsent = recordValues.get("genomic_research_consent");
		Object genomicSigned = recordValues.get("genomic_signed");
		Object genomicResearchConsentDate = recordValues.get("genomic_research_consent_date");
		if (genomicResearchConsent == null || genomicResearchConsent.toString().isEmpty() || genomicResearchConsent.equals("Not Ticked")) {
			setConsentStatusPending(customFields);
			return;
		} else if (genomicResearchConsent.equals("Yes") && (genomicSigned == null || genomicSigned.equals("No") || genomicResearchConsentDate == null)) {
			setConsentStatusPending(customFields);
			return;
		}

		Object newResearchConsent = recordValues.get("new_research_consent");
		Object newSigned = recordValues.get("signed");
		Object newResearchConsentDate = recordValues.get("new_research_consent_date");
		if (newResearchConsent == null || newResearchConsent.toString().isEmpty()) {
			setConsentStatusPending(customFields);
			return;
		} else if (newResearchConsent.equals("Yes") && (newSigned == null || newSigned.equals("No") || newResearchConsentDate == null)) {
			setConsentStatusPending(customFields);
			return;
		}

		if (newResearchConsent.equals("Not Ticked")) {
			setConsentStatusClarify(customFields);
			return;
		}

		if (participant.getGender() == null) {
			setConsentStatusClarify(customFields);
			return;
		}

		Stream<String> customFieldsToCheck = Stream.of("survey_source", "address_line_11", "address_line_21", "suburb", "states", "post_code", "phone_number");
		if (customFieldsToCheck.anyMatch(field -> customFields.getAttrValue(field) == null)) {
			setConsentStatusClarify(customFields);
			return;
		}

		Stream<String> translatorFormFields   = Stream.of("translator_given_name", "translator_family_name", "translator_signed", "date_translator_signed");
		Stream<String> investigatorFormFields = Stream.of("investigator_given_name", "investigator_family_name", "date_of_confirmation");

		boolean allTranslatorFieldsEmpty   = translatorFormFields.allMatch(field -> recordValues.get(field) == null);
		boolean someTranslatorFieldsEmpty  = translatorFormFields.anyMatch(field -> recordValues.get(field) == null);

		boolean allInvestigatorFieldsEmpty = investigatorFormFields.allMatch(field -> recordValues.get(field) == null);
		boolean someInvestigatorFieldsEmpty= investigatorFormFields.anyMatch(field -> recordValues.get(field) == null);
		if (allTranslatorFieldsEmpty && allInvestigatorFieldsEmpty) {
			setConsentStatusClarify(customFields);
			return;
		} else if (allTranslatorFieldsEmpty) {
			if (someInvestigatorFieldsEmpty) {
				setConsentStatusClarify(customFields);
				return;
			}
		} else if (allInvestigatorFieldsEmpty) {
			if (someTranslatorFieldsEmpty) {
				setConsentStatusClarify(customFields);
				return;
			}
		} else {
			if (someInvestigatorFieldsEmpty || someTranslatorFieldsEmpty) {
				setConsentStatusClarify(customFields);
				return;
			}
		}

		if (recordValues.get("form_version") == null) {
			setConsentStatusClarify(customFields);
			return;
		}

		Object notifyImplications = recordValues.get("notify_implications");
		if ("Not Ticked".equals(notifyImplications)) {
			setConsentStatusClarify(customFields);
			return;
		}

		if ("Yes".equals(notifyImplications)) {
			Object contactNominatedPerson = recordValues.get("contact_nominated_person");
			if ("Not Ticked".equals(contactNominatedPerson)) {
				setConsentStatusClarify(customFields);
				return;
			}

			Stream<String> doctorFields = Stream.of("doctor_given_name", "doctor_family_name", "clinic_name", "address_line_1", "address_line_2", "suburb", "states", "post_code");
			if (doctorFields.anyMatch(field -> recordValues.get(field) == null || recordValues.get(field).toString().isEmpty())) {
				setConsentStatusClarify(customFields);
				return;
			}
		}

		Object alternativeContactNominated = recordValues.get("alternative_contact_nominated");
		if ("Not Ticked".equals(alternativeContactNominated)) {
			setConsentStatusClarify(customFields);
			return;
		}

		if ("Yes".equals(alternativeContactNominated)) {
			List<Map<String, Object>> alternativeContacts = (List<Map<String, Object>>) recordValues.get("alternative_contact_section");
			if (alternativeContacts == null || alternativeContacts.isEmpty()) {
				setConsentStatusClarify(customFields);
			} else {
				Map<String, Object> alternativeContact = alternativeContacts.iterator().next();
				Stream<String> altContactFields = Stream.of("contact_1_given_name", "family_name", "relationship", "address_line_1", "suburb", "state", "post_code", "phone_number", "email_address");
				if (altContactFields.anyMatch(field -> alternativeContact.get(field) == null || alternativeContact.get(field).toString().isEmpty())) {
					setConsentStatusClarify(customFields);
				}
			}
		}

		setConsentStatusActive(customFields);
	}

	private void setConsentStatusPending(DeObject customFields) {
		setAttrValue(customFields, "consent_status", CONSENT_STATUS_PENDING);
		customFields.saveOrUpdate();
	}

	private void setConsentStatusClarify(DeObject customFields) {
		setAttrValue(customFields, "consent_status", CONSENT_STATUS_CLARIFY);
		customFields.saveOrUpdate();
	}

	private void setConsentStatusWithdrawn(DeObject customFields) {
		setAttrValue(customFields, "consent_status", CONSENT_STATUS_WITHDRAWN);
		customFields.saveOrUpdate();
	}

	private void setConsentStatusActive(DeObject customFields) {
		setAttrValue(customFields, "consent_status", CONSENT_STATUS_ACTIVE);
		customFields.saveOrUpdate();
	}

	private Object getAttrValue(DeObject customFields, String name) {
		DeObject.Attr attr = getAttr(customFields, name);
		return attr != null ? attr.getValue() : null;
	}

	private DeObject.Attr getAttr(DeObject customFields, String name) {
		if (customFields == null || customFields.getAttrs() == null || StringUtils.isBlank(name)) {
			return null;
		}

		DeObject.Attr resultAttr = null;
		for (DeObject.Attr attr : customFields.getAttrs()) {
			if (attr.getName().equals(name) || attr.getUdn().equals(name)) {
				resultAttr = attr;
				break;
			}
		}

		return resultAttr;
	}

	private void setAttrValue(DeObject customFields, String name, Object value) {
		DeObject.Attr resultAttr = getAttr(customFields, name);
		if (resultAttr == null) {
			resultAttr = new DeObject.Attr();
			resultAttr.setName(name);
			resultAttr.setUdn(name);
			customFields.getAttrs().add(resultAttr);
		}

		resultAttr.setValue(value);
	}

	private Map<String, Object> toValueMap(FormData formData) {
		Map<String, Object> result = new HashMap<>();
		result.put("recordId", formData.getRecordId());

		for (ControlValue cv : formData.getFieldValues()) {
			Control ctrl = cv.getControl();
			Object value = null;
			if (ctrl instanceof FileUploadControl) {
				FileControlValue fcv = (FileControlValue) cv.getValue();
				value = fcv != null ? fcv.toValueMap() : null;
			} else if (ctrl instanceof SubFormControl) {
				List<FormData> sfDataList = (List<FormData>) cv.getValue();
				value = Utility.nullSafeStream(sfDataList).map(this::toValueMap).collect(Collectors.toList());
			} else if (ctrl instanceof AbstractLookupControl) {
				value = cv.getControl().toDisplayValue(cv.getValue());
			} else {
				if (cv.getValue() instanceof String[]) {
					value = Stream.of((String[]) cv.getValue()).map(ctrl::fromString).collect(Collectors.toList());
				} else {
					value = ctrl.fromString(ctrl.toString(cv.getValue()));
				}
			}

			result.put(ctrl.getUserDefinedName(), value);
		}

		return result;
	}
}
