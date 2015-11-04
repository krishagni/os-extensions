package com.krishagni.openspecimen.unsw.rest;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.FileDetail;
import com.krishagni.openspecimen.unsw.events.IdentifiedSprDetail;
import com.krishagni.openspecimen.unsw.services.IdentifiedSprService;

@Controller
@RequestMapping("/identified-spr")
public class IdentifiedSprController {

	@Autowired
	IdentifiedSprService identifiedSprSvc;
	
	@RequestMapping(method = RequestMethod.GET, value = "/{visitId}/name")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public String getIdentifiedSpr(@PathVariable Long visitId) {
		ResponseEvent<String> resp = identifiedSprSvc.getIdentifiedSprName(getRequest(visitId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{visitId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void DownloadIdentifiedSpr(@PathVariable Long visitId, HttpServletResponse httpResp) {
		ResponseEvent<FileDetail> resp = identifiedSprSvc.getIdentifiedSpr(getRequest(visitId));
		resp.throwErrorIfUnsuccessful();
		FileDetail fileDetail = resp.getPayload();
		File file = new File(fileDetail.getPath()); 
		Utility.sendToClient(httpResp, fileDetail.getFilename(), file);
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/{visitId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public FileDetail uploadSprFile(
		@PathVariable("visitId") 
		Long visitId, 
		
		@PathVariable("file") 
		MultipartFile file)
	throws IOException {
		IdentifiedSprDetail detail = new IdentifiedSprDetail();
		detail.setVisitId(visitId);
		detail.setSpr(file);
		ResponseEvent<FileDetail> resp = identifiedSprSvc.uploadIdentifiedSpr(getRequest(detail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value="/{visitId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public boolean deleteSprFile(@PathVariable("visitId") Long visitId) {
		ResponseEvent<Boolean> resp = identifiedSprSvc.deleteIdentifiedSpr(getRequest(visitId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	private <T> RequestEvent<T> getRequest(T payload) {
		return new RequestEvent<T>(payload);				
	}
	
}
