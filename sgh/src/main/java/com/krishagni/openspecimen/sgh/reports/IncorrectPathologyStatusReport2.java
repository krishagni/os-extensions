package com.krishagni.openspecimen.sgh.reports;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.openspecimen.sgh.report.processor.CustomCsvReportProcessor;
import com.krishagni.openspecimen.sgh.services.OutputCsvFileService;

@Configurable
public class IncorrectPathologyStatusReport2 extends CustomCsvReportProcessor implements ScheduledTask {
	@Autowired
	private OutputCsvFileService opCsvFileSvc;
	
	@Autowired
	private DaoFactory daoFactory;
	
	private List<String> specLabelPrefix = new ArrayList<>();
	
	private String[] dates = new String[] {"", ""};
	
	private static final Log logger = LogFactory.getLog(IncorrectPathologyStatusReport2.class);
	
	private static final String SG_REPORT_DIR = "os_report";
	
	private static final String INPUT_DATE_FORMAT = "dd-mm-yyyy";
	
	private static final String OUTPUT_DATE_FORMAT = "yyyy-mm-dd";

	private static final String ENDING_WITH_B_INT_REGEX = ".*(B([0-9]*)?)$";

	private static final String ENDING_WITH_T_REGEX = ".*T$";

	private static final String ENDING_WITH_N_REGEX = ".*N$";

	@Override
	@PlusTransactional
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		try {
			specLabelPrefix.addAll(Arrays.asList(ENDING_WITH_T_REGEX, ENDING_WITH_N_REGEX, ENDING_WITH_B_INT_REGEX));
			dates = getRtArgs(jobRun).split(" ");
			
			String file = initFile(getFileName(), SG_REPORT_DIR);
			opCsvFileSvc.initCsvWriter(file);
			opCsvFileSvc.writeNext(Arrays.asList(getHeader()));
			
			for (String prefix : specLabelPrefix) {
				List<Object[]> resultSet = executeQuery(INCORRECT_PATH_STATUS_SPECIMEN, getParams(prefix, parseInputDate(dates[0]), parseInputDate(dates[1])));
				
				resultSet.forEach(rs -> {
					opCsvFileSvc.writeNext(Arrays.asList(rs));
					opCsvFileSvc.incrementResultSize();
				});
				
				opCsvFileSvc.flush();
			}
			
			opCsvFileSvc.writeNext(Arrays.asList(System.lineSeparator()));
			opCsvFileSvc.writeNext(Arrays.asList(new String[]{"Total number of incorrect record: " + opCsvFileSvc.getResultSize()}));
			
			jobRun.setLogFilePath(file);
			daoFactory.getScheduledJobDao().saveOrUpdateJobRun(jobRun);
		} catch (Exception e) {
			logger.error("Error occurred while generating 'Incorrect Pathological-Status Report'", e);
		} finally {
			opCsvFileSvc.closeWriter();
		}
	}
	
	private Map<String, Object> getParams(String prefix, String startDate, String endDate) {
		Map<String, Object> params = new HashMap<>();
		params.put("prefix", prefix);
		params.put("startDate", startDate);
		params.put("endDate", endDate);
		
		return params;
	}

	private String getFileName() {
		return "Incorrect_Path_Status_" + Calendar.getInstance().getTimeInMillis() + ".csv";
	}
	
	private String[] getHeader() throws ParseException {
		return new String[] {
				"Duration: " + parseInputDate(dates[0]) + " to " + parseInputDate(dates[1])
				+ System.lineSeparator()
				+ "Report for Incorrect Pathological Status: "
				+ System.lineSeparator()
				+ "CP Short Title, TRID, Specimen Label, Pathological Status, Tissue Site, Tissue Side"
		};
	}
	
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
            "  and vis.collection_timestamp between concat(:startDate, ' 00:00:00') and concat(:endDate, ' 23:59:59') " +
            "order by " + 
            "  spec.label ";
}