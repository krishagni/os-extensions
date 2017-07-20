
package com.krishagni.openspecimen.asig.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;

import com.krishagni.openspecimen.asig.service.AsigService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping("/asig")
public class AsigController {

	@Autowired
	private AsigService asigSvc;
			
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public String registerAsigPatients() {
		asigSvc.createSites();
		asigSvc.createUsers();
		asigSvc.registerPatients();
		return "Success";
	}
}