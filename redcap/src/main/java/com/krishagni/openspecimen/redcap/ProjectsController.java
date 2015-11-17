package com.krishagni.openspecimen.redcap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

@Controller
@RequestMapping("/redcap-projects")
public class ProjectsController {
	
	@Autowired
	private ProjectService projectService;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<ProjectDetail> getProjects(
			@RequestParam(value="cpId", required=true)
			Long cpId) {
		
		RequestEvent<Long> req = new RequestEvent<Long>(cpId);
		ResponseEvent<List<ProjectDetail>> resp = projectService.getProjects(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();		
	}
		
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ProjectDetail createProject(@RequestBody ProjectDetail detail) {
		RequestEvent<ProjectDetail> req = new RequestEvent<ProjectDetail>(detail);
		ResponseEvent<ProjectDetail> resp = projectService.createProject(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.PUT, value="{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ProjectDetail createProject(
			@PathVariable("id")
			Long projectId,
			
			@RequestBody 
			ProjectDetail detail) {
		
		detail.setId(projectId);
		RequestEvent<ProjectDetail> req = new RequestEvent<ProjectDetail>(detail);
		ResponseEvent<ProjectDetail> resp = projectService.updateProject(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	
	@RequestMapping(method = RequestMethod.POST, value="/{id}/instruments")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Boolean> updateProjectInstruments(
			@PathVariable("id")
			Long projectId,
			
			@RequestBody 
			Set<String> instrumentNames) {
		
		return updateInstruments(null, projectId, instrumentNames);
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/{id}/data")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Boolean> updateProjectData(
			@PathVariable("id")
			Long projectId,
			
			@RequestBody 
			UpdateDataOp op) {
		
		op.setProjectId(projectId);
		return updateProjectData(op);
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/collection-protocols/{id}/instruments")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Boolean> updateCpInstruments(
			@PathVariable("id")
			Long cpId,
			
			@RequestBody 
			Set<String> instrumentNames) {
		
		return updateInstruments(cpId, null, instrumentNames);		
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/collection-protocols/{id}/data")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Boolean> updateCpData(
			@PathVariable("id")
			Long cpId,
			
			@RequestBody 
			UpdateDataOp op) {
		
		op.setCpId(cpId);
		return updateProjectData(op);
	}

	private Map<String, Boolean> updateInstruments(Long cpId, Long projectId, Set<String> instrumentNames) {
		UpdateInstrumentsOp op = new UpdateInstrumentsOp();
		op.setCpId(cpId);
		op.setProjectId(projectId);		
		op.setInstrumentNames(instrumentNames);
		
		RequestEvent<UpdateInstrumentsOp> req = new RequestEvent<UpdateInstrumentsOp>(op);
		ResponseEvent<Void> resp = projectService.updateInstruments(req);
		resp.throwErrorIfUnsuccessful();
		
		return Collections.singletonMap("status", true);		
	}
	
	private Map<String, Boolean> updateProjectData(UpdateDataOp op) {
		RequestEvent<UpdateDataOp> req = new RequestEvent<UpdateDataOp>(op);
		ResponseEvent<Void> resp = projectService.updateData(req);
		resp.throwErrorIfUnsuccessful();
		
		return Collections.singletonMap("status", true);		
	}
}