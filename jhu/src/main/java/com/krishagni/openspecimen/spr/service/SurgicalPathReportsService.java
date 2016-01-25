package com.krishagni.openspecimen.spr.service;

import java.util.List;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.openspecimen.spr.events.LabHeader;
import com.krishagni.openspecimen.spr.events.SprCrit;
import com.krishagni.openspecimen.spr.events.SprDetailCrit;
import com.krishagni.openspecimen.spr.events.SprReport;


public interface SurgicalPathReportsService {

	public ResponseEvent<List<LabHeader>> getReports(RequestEvent<SprCrit> req);
	
	public ResponseEvent<SprReport> getReportDetails(RequestEvent<SprDetailCrit> req);
	
}
