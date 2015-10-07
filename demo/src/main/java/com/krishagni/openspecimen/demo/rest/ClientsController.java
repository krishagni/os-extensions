package com.krishagni.openspecimen.demo.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.openspecimen.demo.events.ClientDetail;
import com.krishagni.openspecimen.demo.services.ClientService;

@Controller
@RequestMapping("/demo")
public class ClientsController {
  @Autowired
  private ClientService clientSvc;
  
  @RequestMapping(method = RequestMethod.POST, value = "/sign-up")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ClientDetail sendSignUpMail (@RequestBody ClientDetail detail) {
		ResponseEvent<ClientDetail> resp = clientSvc.createUser(new RequestEvent<ClientDetail>(detail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
  }
}
