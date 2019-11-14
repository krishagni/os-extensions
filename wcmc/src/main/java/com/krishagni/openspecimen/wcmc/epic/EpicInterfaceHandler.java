package com.krishagni.openspecimen.wcmc.epic;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.krishagni.catissueplus.core.biospecimen.events.StagedParticipantDetail;

public abstract class EpicInterfaceHandler {
	private static final Log logger = LogFactory.getLog(EpicInterfaceHandler.class);

	private EpicSourceDbManager sourceDbManager;

	public EpicInterfaceHandler() {
		try {
			sourceDbManager = new EpicSourceDbManager();
			
			sourceDbManager.connect(
					EpicPropConfig.getInstance().getDbUrl(),
					EpicPropConfig.getInstance().getDbUser(),
					EpicPropConfig.getInstance().getDbPwd());
		
			sourceDbManager.loadJson(EpicPropConfig.getInstance().getJsonMapping());
		} catch (IOException e) {
			logger.error("Error loading the JSON file", e);
		}
	}

	public StagedParticipantDetail getStagedParticipants() {
		// 
		return null;
	}
}
