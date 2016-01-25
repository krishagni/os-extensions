package com.krishagni.openspecimen.epic.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.openspecimen.epic.service.EpicService;

@Controller
@RequestMapping("/epic")
public class EpicController {

		@Autowired
		private EpicService epicSvc;
		
		@RequestMapping(method = RequestMethod.POST)
		@ResponseStatus(HttpStatus.OK)
		@ResponseBody	
		public String generateAndPrintTrids() {
			epicSvc.registerParticipants();
			return "Success";
		}
	
}
