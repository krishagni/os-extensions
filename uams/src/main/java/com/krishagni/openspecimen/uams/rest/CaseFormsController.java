package com.krishagni.openspecimen.uams.rest;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

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
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.openspecimen.uams.services.CaseFormsGenerator;

@Controller
@RequestMapping("/uams/case-forms")
public class CaseFormsController {

	@Autowired
	private CaseFormsGenerator generator;

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> generateCaseForms(@RequestBody Map<String, Object> input) {
		if (input == null || input.isEmpty()) {
			input = Collections.singletonMap("count", 1);
		}

		File caseFormsFile = ResponseEvent.unwrap(generator.generateCaseForms(RequestEvent.wrap(input)));
		return Collections.singletonMap("fileId", caseFormsFile != null ? caseFormsFile.getName() : null);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/files")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void getFile(@RequestParam(value = "fileId") String fileId, HttpServletResponse httpResp) {
		File file = ResponseEvent.unwrap(generator.getFile(RequestEvent.wrap(fileId)));
		Utility.sendToClient(httpResp, "case_forms.pdf", file);
	}
}
