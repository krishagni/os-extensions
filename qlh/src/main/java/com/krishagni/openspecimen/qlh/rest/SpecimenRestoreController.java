package com.krishagni.openspecimen.qlh.rest;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.openspecimen.qlh.biospecimen.SpecimenEventRestoreDetail;
import com.krishagni.openspecimen.qlh.biospecimen.SpecimenRestoreDetail;
import com.krishagni.openspecimen.qlh.biospecimen.SpecimenRestorer;

@Controller
@RequestMapping("/qlh-restore-specimens")
public class SpecimenRestoreController {

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, String> restoreSpecimens(@RequestBody SpecimenRestoreDetail input) {
		if (input.getFrom() != null) {
			input.setFrom(Utility.chopTime(input.getFrom()));
		} else {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Start date is required.");
		}

		if (input.getTo() != null) {
			input.setTo(Utility.getEndOfDay(input.getTo()));
		} else {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "End date is required.");
		}

		if (StringUtils.isBlank(input.getOverwrittenBy())) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Overwritten by user login name is required");
		}

		new SpecimenRestorer().restoreSpecimens(input);
		return Collections.singletonMap("success", "true");
	}


	@RequestMapping(method = RequestMethod.POST, value = "events")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, String> restoreSpecimenEvents(@RequestBody SpecimenEventRestoreDetail input) {
		if (input.getRestoreUntil() != null) {
			input.setRestoreUntil(Utility.getEndOfDay(input.getRestoreUntil()));
		} else {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Restore until date is required.");
		}

		if (StringUtils.isBlank(input.getOverwrittenBy())) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Overwritten by user login name is required");
		}

		if (CollectionUtils.isEmpty(input.getSpecimenIds())) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Specimen IDs whose events need to be restored is required");
		}

		new SpecimenRestorer().restoreSpecimenEvents(input);
		return Collections.singletonMap("success", "true");
	}
}
