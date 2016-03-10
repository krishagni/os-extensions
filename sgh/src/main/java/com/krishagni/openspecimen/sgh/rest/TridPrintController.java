package com.krishagni.openspecimen.sgh.rest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.openspecimen.sgh.events.BulkTridPrintOpDetail;
import com.krishagni.openspecimen.sgh.events.TridsRePrintOpDetail;
import com.krishagni.openspecimen.sgh.services.TridPrintSvc;

@Controller
@RequestMapping("/sgh/trids")
public class TridPrintController {
	@Autowired
	private TridPrintSvc tridPrintSvc;
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public Boolean generateAndPrintTrids(@RequestBody BulkTridPrintOpDetail detail) {
		RequestEvent<BulkTridPrintOpDetail> req = new RequestEvent<BulkTridPrintOpDetail>(detail);
		ResponseEvent<Boolean> resp = tridPrintSvc.generateAndPrintTrids(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/print")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public Boolean rePrintTrids(@RequestParam(value = "printer", required = false)
	String printerName,@RequestBody TridsRePrintOpDetail detail) {
		RequestEvent<TridsRePrintOpDetail> req = new RequestEvent<TridsRePrintOpDetail>(detail);
		req.getPayload().setPrinterName(printerName);
		ResponseEvent<Boolean> resp = tridPrintSvc.printTrids(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}

}
