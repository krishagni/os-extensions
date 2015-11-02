package com.krishagni.openspecimen.unsw.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.openspecimen.unsw.events.IdentifiedSprDetail;
import com.krishagni.openspecimen.unsw.services.IdentifiedSprService;

@Controller
@RequestMapping("/identified-spr")
public class IdentifiedSprController {

	@Autowired
	IdentifiedSprService identifiedSprSvc;
	
	@RequestMapping(method = RequestMethod.GET, value = "/{visitId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public IdentifiedSprDetail getIdentifiedSpr(@PathVariable Long visitId) {
		ResponseEvent<IdentifiedSprDetail> resp = identifiedSprSvc.getIdentifiedSprDetail(new RequestEvent<Long>(visitId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}
