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
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.openspecimen.sgh.report.processor.CustomCsvReportProcessor;

@Configurable
public class IncorrectTissueSiteReport2 extends CustomCsvReportProcessor implements ScheduledTask {
	@Autowired
	private ConfigurationService cfgSvc;

	private static final String SG_REPORT_DIR = "os_report";
	
	private static final String INPUT_DATE_FORMAT = "dd-MM-yyyy";

	private String[] dates;
	
	private Date[] parsedDates = new Date[2];

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
				+ "Report for Incorrect Tissue Site and Side : "
				+ System.lineSeparator()
				+ "CP Short Title, TRID, Specimen Label, Tissue Site, Tissue Side"
		};
	}

	@Override
	public Map<String, List<Map<String, Object>>> getQueries() {
		Map<String, Object> params = new HashMap<>();
		params.put("startDate", parsedDates[0]);
		params.put("endDate", parsedDates[1]);
		
		List<Map<String,Object>> paramList = new ArrayList<>();
		paramList.add(params);

		Map<String, List<Map<String, Object>>> queries = new HashMap<>();
		queries.put(QUERY, paramList);

		return Collections.singletonMap(QUERY, paramList);
	}

	@Override
	public void postProcess(CsvFileWriter writer, int totalRows) {
		writer.writeNext(new String[]{System.lineSeparator()});
		writer.writeNext(new String[]{"Total number of incorrect record: " + totalRows});
	}

	private String getFileName() {
		return "Incorrect_tissue_site_and_side_" + Calendar.getInstance().getTimeInMillis() + ".csv";
	}

	private String getDirPath(String dirName) {
		return cfgSvc.getDataDir() + File.separator + dirName;
	}

	private Date parseInputDate(String date) throws ParseException {
		return new SimpleDateFormat(INPUT_DATE_FORMAT).parse(date);
	}

	private static final String QUERY =
			"select " +
			"  cp.short_title as cp_short_title, s.name as trid, " + 
			"  spmn.label as specimen_label, spmn.tissue_site as tissue_site, " + 
			"  spmn.tissue_side as tissue_side " +
			"from " +
			"  catissue_specimen spmn " +
			"  join (" +
			"    select " +
			"      scg.identifier, scg.collection_timestamp, " +
			"      scg.name, scg.collection_protocol_reg_id " +
			"    from " +
			"      catissue_specimen_coll_group scg " +
			"      join catissue_specimen spec on spec.specimen_collection_group_id = scg.identifier " +
			"    where " +
			"      spec.activity_status in ('Active', 'Closed') and " + 
			"      spec.lineage = 'New' " +
			"    group by " + 
			"      scg.name " +
			"    having " +
			"      count(distinct spec.tissue_site) > 1 or " +
			"      count(distinct spec.tissue_side) > 1 " +
			"  ) as s on spmn.specimen_collection_group_id = s.identifier " +
			"  join catissue_coll_prot_reg ccpr on ccpr.identifier = s.collection_protocol_reg_id " +
			"  join catissue_collection_protocol cp on cp.identifier = ccpr.collection_protocol_id " +
			"where " + 
			"  spmn.lineage = 'New' and " +
			"  spmn.label is not null and " +
			"  spmn.collection_status = 'Collected' and " +
			"  spmn.activity_status = 'Active' and " +
			"  s.collection_timestamp between :startDate and :endDate " +
			"order by " +
			"  spmn.label ";
}
