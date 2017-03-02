package com.krishagni.os.jhuepic;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import com.krishagni.catissueplus.core.init.ImportForms;

import krishagni.catissueplus.beans.FormContextBean;

public class ImportParticipantForm extends ImportForms {
	@Override
	protected Collection<String> listFormFiles() throws IOException {
		return Collections.singleton(CUSTOM_PARTICIPANT_FORM);
	}

	@Override
	protected FormContextBean getFormContext(String formFile, Long formId) {
		FormContextBean formCtx = getDaoFactory().getFormDao().getFormContext(formId, -1L, "ParticipantExtension");
		if (formCtx == null) {
			formCtx = new FormContextBean();
		}

		formCtx.setContainerId(formId);
		formCtx.setCpId(-1L);
		formCtx.setEntityType("ParticipantExtension");
		formCtx.setSysForm(false);
		formCtx.setMultiRecord(false);
		formCtx.setSortOrder(null);
		return formCtx;
	}

	@Override
	protected void cleanup() {

	}

	@Override
	protected boolean isSysForm(String formFile) {
		return false;
	}

	@Override
	public boolean isCreateTable() {
		return true;
	}

	private static final String CUSTOM_PARTICIPANT_FORM = "/com/krishagni/os/jhuepic/external-mrn.xml";
}
