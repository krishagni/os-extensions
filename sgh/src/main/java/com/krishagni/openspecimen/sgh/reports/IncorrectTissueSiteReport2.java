package com.krishagni.openspecimen.sgh.reports;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.openspecimen.sgh.report.processor.CustomCsvReportProcessor;

@Configurable
public class IncorrectTissueSiteReport2 extends CustomCsvReportProcessor implements ScheduledTask {
	private static final String SG_REPORT_DIR = "os_report";
	
	private static final String INPUT_DATE_FORMAT = "dd-mm-yyyy";
	
	private static final String OUTPUT_DATE_FORMAT = "yyyy-mm-dd";

	@Override
	@PlusTransactional
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		String fileName = getFileName();

		String[] dates = getRtArgs(jobRun).split(" ");
		String[] header = getHeader(dates);

		Map<String, List<Map<String, Object>>> queriesAndParams = getQueriesAndParamsMap(QUERY, dates);

		super.process(jobRun, fileName, SG_REPORT_DIR, header, queriesAndParams);
	}

	private String getRtArgs(ScheduledJobRun jobRun) {
		String rtArgs = jobRun.getRtArgs();
		if (rtArgs != null && !rtArgs.isEmpty()) {
			return rtArgs;
		}
		return null;
	}

	private Map<String, List<Map<String, Object>>> getQueriesAndParamsMap(String query, String[] dates) throws ParseException {
		Map<String, Object> params = new HashMap<>();
		params.put("startDate", parseInputDate(dates[0]));
		params.put("endDate", parseInputDate(dates[1]));
		
		List<Map<String,Object>> paramList = new ArrayList<>();
		paramList.add(params);

		Map<String, List<Map<String, Object>>> queriesAndParams = new HashMap<>();
		queriesAndParams.put(query, paramList);

		return queriesAndParams;
	}

	private String[] getHeader(String[] dates) throws ParseException {
		return new String[] {
				"Duration: " + parseInputDate(dates[0]) + " to " + parseInputDate(dates[1])
				+ System.lineSeparator()
				+ "Report for Incorrect Tissue Site and Side : "
				+ System.lineSeparator()
				+ "CP Short Title, TRID, Specimen Label, Tissue Site, Tissue Side"
		};
	}

	private String getFileName() {
		return "Incorrect_tissue_site_and_side_" + Calendar.getInstance().getTimeInMillis() + ".csv";
	}

//	private Date parseInputDate(String date) throws ParseException {
//		return new SimpleDateFormat(INPUT_DATE_FORMAT).parse(date);
//	}

	private String parseInputDate(String date) throws ParseException {
		SimpleDateFormat inputFormat = new SimpleDateFormat(INPUT_DATE_FORMAT);
		SimpleDateFormat outputFormat = new SimpleDateFormat(OUTPUT_DATE_FORMAT);
		
		return outputFormat.format(inputFormat.parse(date));
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
			"  s.collection_timestamp between concat( :startDate , ' 00:00:00' ) and concat( :endDate , ' 23:59:59' ) " + 
			"order by " +
			"  spmn.label "; 

}
