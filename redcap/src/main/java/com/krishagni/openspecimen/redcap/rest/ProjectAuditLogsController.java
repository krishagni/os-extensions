package com.krishagni.openspecimen.redcap.rest;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.openspecimen.redcap.events.ProjectAuditLogDetail;
import com.krishagni.openspecimen.redcap.services.ProjectService;

@Controller
@RequestMapping("/redcap-project-audit-logs")
public class ProjectAuditLogsController {
	
	@Autowired
	private ProjectService projectService;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<ProjectAuditLogDetail> getProjects(
			@RequestParam(value="cpId", required=true)
			Long cpId) {
		
		RequestEvent<Long> req = new RequestEvent<Long>(cpId);
		ResponseEvent<List<ProjectAuditLogDetail>> resp = projectService.getProjectAuditLogs(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();		
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}/failed-events-log")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void downloadFailedEventsLog(
			@PathVariable("id")
			Long auditLogId,
			
			HttpServletResponse httpResp) {
		
		RequestEvent<Long> req = new RequestEvent<Long>(auditLogId);
		ResponseEvent<File> resp = projectService.getFailedEventsLogFile(req);
		resp.throwErrorIfUnsuccessful();
		
		if (resp.getPayload() != null) {
			File file = resp.getPayload();
			Utility.sendToClient(httpResp, file.getName(), resp.getPayload());
		}
	}
}