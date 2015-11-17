package com.krishagni.openspecimen.redcap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

//
// temp service
//

@Controller
@RequestMapping("/redcap-events")
public class EventEmitter {
	private String[] cohorts = {"C01", "C02"};
	
	private String barcodeFmt = "CM-%s-S-S-%s";
	
	private String dvFmt = "clinical_barcode = '%s',\nstudy_id = 'CM',\nsubject_id = 'CM-%s',\nvisit_id = 'S',\nclinical_barcode_complete = '2'";
			
	private String projectId = "5911";
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<Map<String, String>> events(@RequestBody String req) {
		System.err.println("Request is: " + req);
		
		List<Map<String, String>> events = new ArrayList<Map<String,String>>();
		for (int i = 0; i < new Random().nextInt(2) + 1; ++i) {
			String subjectId = String.format("%04d", new Random().nextInt(1000));
			String cohort = cohorts[new Random().nextInt(2)];
			
			String barcode = String.format(barcodeFmt, subjectId, cohort);
			String dv = String.format(dvFmt, barcode, subjectId);
			
			Map<String, String> event = new HashMap<String, String>();
			event.put("project_id", projectId);
			event.put("ts", new SimpleDateFormat("yyyyMMddhhmmss").format(Calendar.getInstance().getTime()));
			event.put("event", "INSERT");
			event.put("pk", barcode);
			event.put("object_type", "redcap_data");
			event.put("data_values", dv);
			events.add(event);
			
			System.err.println("*** Generated: " + barcode);
		}
		
		return events;		
	}
	
//	public void process(
//			@RequestBody
//			List<Map<String, String>> inputEvents) {
//		List<LogEvent> rcEvents = new ArrayList<LogEvent>();
//		
//		for (Map<String, String> inputEvent : inputEvents) {
//			rcEvents.add(LogEvent.parse(inputEvent));
//		}
//		
//		eventHandler.processEvents(rcEvents);
//	}
}
