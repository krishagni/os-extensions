package com.krishagni.openspecimen.washu.rest;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;
import com.krishagni.openspecimen.washu.services.ReportGenerator;

@Controller
@RequestMapping("/washu-reports")
public class ReportsController {

	@Autowired
	private ReportGenerator rptsGenerator;

	@RequestMapping(method = RequestMethod.GET, value="/working-specimens")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public QueryDataExportResult exportWorkingSpecimensReport(
		@RequestParam(value = "listId")
		Long listId,

		@RequestParam(value = "specimenId", required = false)
		List<Long> specimenIds) {

		SpecimenListCriteria crit = new SpecimenListCriteria().specimenListId(listId).ids(specimenIds);
		return response(rptsGenerator.exportWorkingSpecimensReport(request(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value="/order-report")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public QueryDataExportResult exportOrderReport(@RequestParam(value = "orderId") Long orderId) {
		return response(rptsGenerator.exportOrderReport(request(new EntityQueryCriteria(orderId))));
	}

	@RequestMapping(method = RequestMethod.GET, value="/request-report")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public QueryDataExportResult exportRequestReport(
		@RequestParam(value = "requestId")
		Long requestId,

		@RequestParam(value = "specimenId", required = false)
		List<Long> specimenIds) {

		EntityQueryCriteria crit = new EntityQueryCriteria(requestId);
		crit.setParams(Collections.singletonMap("specimenIds", specimenIds));
		return response(rptsGenerator.exportRequestReport(request(crit)));
	}

	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}
