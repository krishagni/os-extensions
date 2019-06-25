package com.krishagni.openspecimen.sgh.reports;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
public class IncorrectPathologyStatusReport2 extends CustomCsvReportProcessor implements ScheduledTask {
	private List<String> specLabelPrefix = new ArrayList<>(Arrays.asList(ENDING_WITH_T_REGEX, ENDING_WITH_N_REGEX, ENDING_WITH_B_INT_REGEX));
	
	private static final String SG_REPORT_DIR = "os_report";
	
	private static final String INPUT_DATE_FORMAT = "dd-mm-yyyy";
	
	private static final String OUTPUT_DATE_FORMAT = "yyyy-mm-dd";

	private static final String ENDING_WITH_B_INT_REGEX = ".*(B([0-9]*)?)$";

	private static final String ENDING_WITH_T_REGEX = ".*T$";

	private static final String ENDING_WITH_N_REGEX = ".*N$";

	@Override
	@PlusTransactional
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		String fileName = getFileName();

		String[] dates = getRtArgs(jobRun).split(" ");
		String[] header = getHeader(dates);

		Map<String, List<Map<String, Object>>> queriesAndParams = getQueriesAndParamsMap(INCORRECT_PATH_STATUS_SPECIMEN, dates, specLabelPrefix);

		super.process(jobRun, fileName, SG_REPORT_DIR, header, queriesAndParams);
	}

	private String getRtArgs(ScheduledJobRun jobRun) {
		String rtArgs = jobRun.getRtArgs();
		if (rtArgs != null && !rtArgs.isEmpty()) {
			return rtArgs;
		}
		return null;
	}

	private Map<String, List<Map<String, Object>>> getQueriesAndParamsMap(String query, String[] dates, List<String> specLabelPrefix) throws ParseException {
		Map<String, List<Map<String, Object>>> queriesAndParams = new HashMap<>();
		List<Map<String,Object>> paramList = new ArrayList<>();
		
		for (String prefix : specLabelPrefix) {
			Map<String, Object> params = new HashMap<>();
			params.put("prefix", prefix);
			params.put("startDate", parseInputDate(dates[0]));
			params.put("endDate", parseInputDate(dates[1]));

			paramList.add(params);
		}

		queriesAndParams.put(query, paramList);
		return queriesAndParams;
	}

	private String getFileName() {
		return "Incorrect_Path_Status_" + Calendar.getInstance().getTimeInMillis() + ".csv";
	}
	
	private String[] getHeader(String[] dates) throws ParseException {
		return new String[] {
				"Duration: " + dates[0] + " to " + dates[1]
				+ System.lineSeparator()
				+ "Report for Incorrect Pathological Status: "
				+ System.lineSeparator()
				+ "CP Short Title, TRID, Specimen Label, Pathological Status, Tissue Site, Tissue Side"
		};
	}
	
//	private Date parseInputDate(String date) throws ParseException {
//		return new SimpleDateFormat(INPUT_DATE_FORMAT).parse(date);
//	}

	private String parseInputDate(String date) throws ParseException {
		SimpleDateFormat inputFormat = new SimpleDateFormat(INPUT_DATE_FORMAT);
		SimpleDateFormat outputFormat = new SimpleDateFormat(OUTPUT_DATE_FORMAT);
		
		return outputFormat.format(inputFormat.parse(date));
	}
	
	private static final String INCORRECT_PATH_STATUS_SPECIMEN = 
			"select " + 
            "  vis.cp_short_title, vis.name, spec.label, " +
            "  spec.pathological_status, spec.tissue_site, spec.tissue_side " + 
            "from " + 
            "  catissue_specimen spec " + 
            "  join ( " + 
            "    select " + 
            "      cp.short_title as cp_short_title, visit.name, visit.identifier, visit.collection_timestamp " + 
            "    from " + 
            "      catissue_specimen spec " + 
            "      left join catissue_specimen_coll_group visit on visit.identifier = spec.specimen_collection_group_id " + 
            "      left join catissue_coll_prot_reg cpr on cpr.identifier = visit.collection_protocol_reg_id " + 
            "      left join catissue_collection_protocol cp on cp.identifier = cpr.collection_protocol_id " + 
            "    where " +
            "      spec.activity_status != 'Disabled' and spec.lineage = 'New' and spec.label regexp :prefix " + 
            "    group by " +
            "      cp.short_title, visit.name, visit.identifier " + 
            "    having " +
            "      count(distinct spec.pathological_status) > 1 " + 
            "  ) vis on vis.identifier = spec.specimen_collection_group_id " + 
            "where " +
            "  spec.label regexp :prefix " +
            "  and vis.collection_timestamp between concat( :startDate , ' 00:00:00' ) and concat( :endDate , ' 23:59:59' ) " +
            "order by " + 
            "  spec.label ";
}