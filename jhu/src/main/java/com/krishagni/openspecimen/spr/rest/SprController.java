package com.krishagni.openspecimen.spr.rest;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.openspecimen.spr.events.LabHeader;
import com.krishagni.openspecimen.spr.events.SprCrit;
import com.krishagni.openspecimen.spr.events.SprDetailCrit;
import com.krishagni.openspecimen.spr.events.SprReport;
import com.krishagni.openspecimen.spr.service.SurgicalPathReportsService;

@Controller
@RequestMapping("/jhu/sprs")
public class SprController {

	@Autowired
	private SurgicalPathReportsService sprSvc;
	
	@Autowired
	private HttpServletRequest httpServletRequest;

  @RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public List<LabHeader> getSprList(
			@RequestParam(value = "mrn", required= false) String mrn) {
		SprCrit crit = new SprCrit();
		crit.setMrn(mrn);
		RequestEvent<SprCrit> req = new RequestEvent<SprCrit>(crit);
		ResponseEvent<List<LabHeader>> resp = sprSvc.getReports(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}
	
  @RequestMapping(method = RequestMethod.GET, value = "/{mrn}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SprReport getSprDetail(@PathVariable("mrn") String mrn, 
			@RequestParam(value = "pathId", required= true) String pathId) {
		SprDetailCrit crit = new SprDetailCrit();
		crit.setMrn(mrn);
		crit.setPathId(pathId);
		RequestEvent<SprDetailCrit> req = new RequestEvent<SprDetailCrit>(crit);
		ResponseEvent<SprReport> resp = sprSvc.getReportDetails(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}
}
