package com.krishagni.openspecimen.washu.services;

import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;

public interface ReportGenerator {
	ResponseEvent<QueryDataExportResult> exportWorkingSpecimensReport(RequestEvent<SpecimenListCriteria> req);

	ResponseEvent<QueryDataExportResult> exportOrderReport(RequestEvent<EntityQueryCriteria> req);
}
