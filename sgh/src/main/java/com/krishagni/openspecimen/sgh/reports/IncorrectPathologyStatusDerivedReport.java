package com.krishagni.openspecimen.sgh.reports;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.openspecimen.sgh.report.processor.CustomCsvReportProcessor;

@Configurable
public class IncorrectPathologyStatusDerivedReport extends CustomCsvReportProcessor implements ScheduledTask {
	@Autowired
	private ConfigurationService cfgSvc;

	private String[] dates;

	private Date[] parsedDates = new Date[2];

	private static final String SG_REPORT_DIR = "os_report";
	
	private static final String INPUT_DATE_FORMAT = "dd-MM-yyyy";
	
	@Override
	@PlusTransactional
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		dates = jobRun.getRtArgs().split(" ");
		parsedDates[0] = parseInputDate(dates[0]);
		parsedDates[1] = parseInputDate(dates[1]);

		process(jobRun);
	}

	@Override
	public String getReportPath() {
		return getDirPath(SG_REPORT_DIR) + File.separator + getFileName();
	}

	@Override
	public String[] getHeader() {
		return new String[] {
				"Duration: " + dates[0] + " to " + dates[1]
				+ System.lineSeparator()
				+ "Report for Incorrect Pathological Status (Derived): "
				+ System.lineSeparator()
				+ "CP Short Title, TRID, Specimen Label, Pathological Status, Tissue Site, Tissue Side"
		};
	}

	@Override
	public Map<String, List<Map<String, Object>>> getQueries() {
		List<Map<String,Object>> paramList = new ArrayList<>();

		Map<String, Object> params = new HashMap<>();
		params.put("startDate", parsedDates[0]);
		params.put("endDate", parsedDates[1]);

		paramList.add(params);

		return Collections.singletonMap(INCORRECT_PATH_STATUS_DERIVED_SPECIMEN, paramList);
	}

	@Override
	public void postProcess(CsvWriter writer, int totalRows) {
		writer.writeNext(new String[]{System.lineSeparator()});
		writer.writeNext(new String[]{"Total number of incorrect record: " + totalRows});
	}

	private String getDirPath(String dirName) {
		return cfgSvc.getDataDir() + File.separator + dirName;
	}

	private String getFileName() {
		return "Incorrect_Path_Status_Derived_" + Calendar.getInstance().getTimeInMillis() + ".csv";
	}
	
	private Date parseInputDate(String date) throws ParseException {
		return new SimpleDateFormat(INPUT_DATE_FORMAT).parse(date);
	}

	private static final String INCORRECT_PATH_STATUS_DERIVED_SPECIMEN = 
			"select " + 
			"  cp.short_title, visit.name, spec.label, " + 
			"  spec.pathological_status, spec.tissue_site, spec.tissue_side " + 
			"from " + 
			"  catissue_specimen spec " + 
			"  left join catissue_specimen_coll_group visit on visit.identifier = spec.specimen_collection_group_id " + 
			"  left join catissue_coll_prot_reg cpr on cpr.identifier = visit.collection_protocol_reg_id " + 
			"  left join catissue_collection_protocol cp on cp.identifier = cpr.collection_protocol_id " + 
			"where " + 
			"  spec.activity_status != 'Disabled' " + 
			"  and spec.lineage = 'Derived' " + 
			"  and visit.collection_timestamp between :startDate and :endDate " + 
			"  and spec.parent_specimen_id in ( " + 
			"    select " + 
			"      parent_specimen_id " + 
			"    from " + 
			"      catissue_specimen s " + 
			"    where " + 
			"      s.activity_status != 'Disabled' " + 
			"      and s.lineage = 'Derived' " + 
			"    group by " + 
			"      s.parent_specimen_id " + 
			"    having " + 
			"      count(distinct s.pathological_status) > 1 " + 
			"  ) " + 
			"order by " + 
			"  spec.label";
}