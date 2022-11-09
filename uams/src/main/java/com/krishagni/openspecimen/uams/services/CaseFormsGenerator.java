package com.krishagni.openspecimen.uams.services;

import java.io.File;
import java.util.Map;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public interface CaseFormsGenerator {
	ResponseEvent<File> generateCaseForms(RequestEvent<Map<String, Object>> req);

	ResponseEvent<File> getFile(RequestEvent<String> fileId);
}
