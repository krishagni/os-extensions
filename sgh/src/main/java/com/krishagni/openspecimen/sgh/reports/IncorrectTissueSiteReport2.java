package com.krishagni.openspecimen.sgh.reports;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
public class IncorrectTissueSiteReport2 extends CustomCsvReportProcessor implements ScheduledTask {
	@Autowired
	private OutputCsvFileService opCsvFileSvc;
	
	@Autowired
	private DaoFactory daoFactory;
	
	private static final Log logger = LogFactory.getLog(IncorrectTissueSiteReport2.class);
	
	private static final String SG_REPORT_DIR = "os_report";
	
	private static final String INPUT_DATE_FORMAT = "dd-mm-yyyy";
	
	private static final String OUTPUT_DATE_FORMAT = "yyyy-mm-dd";

	@Override
	@PlusTransactional
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		try {
			String[] dates = {"", ""};
			dates = getRtArgs(jobRun).split(" ");
			
			String file = initFile(getFileName(), SG_REPORT_DIR);
			opCsvFileSvc.initCsvWriter(file);
			opCsvFileSvc.writeNext(Arrays.asList(getHeader(dates)));
			
			List<Object[]> resultSet = executeQuery(QUERY, getParams(dates));
			resultSet.forEach(rs -> {
				opCsvFileSvc.writeNext(Arrays.asList(rs));
				opCsvFileSvc.incrementResultSize();
			});
			
			opCsvFileSvc.writeNext(Arrays.asList(System.lineSeparator()));
			opCsvFileSvc.writeNext(Arrays.asList(new String[]{"Total number of incorrect record: " + opCsvFileSvc.getResultSize()}));
			
			jobRun.setLogFilePath(file);
			daoFactory.getScheduledJobDao().saveOrUpdateJobRun(jobRun);
		} catch (Exception e) {
			logger.error("Error occurred while generating 'Incorrect Tissue Site and Side Report'", e);
		} finally {
			opCsvFileSvc.closeWriter();
		}
	}

	private Map<String, Object> getParams(String[] dates) throws ParseException {
		Map<String, Object> params = new HashMap<>();
		params.put("startDate", parseInputDate(dates[0]));
		params.put("endDate", parseInputDate(dates[1]));
		
		return params;
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
			"  s.collection_timestamp between concat(:startDate , ' 00:00:00' ) and concat(:endDate, ' 23:59:59' ) " + 
			"order by " +
			"  spmn.label "; 

}
