package com.krishagni.openspecimen.sgh.reports;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;

import au.com.bytecode.opencsv.CSVWriter;

@Configurable
public class IncorrectPathologyStatusReport implements ScheduledTask {
	@Autowired
	private SessionFactory sessionFactory;
	
	@Autowired
	private ConfigurationService cfgSvc;
	
	@Autowired
	private DaoFactory daoFactory;
	
	private List<String> specLabelPrefix = new ArrayList<>();
	
	private String[] dates = new String[] {"", ""};
	
	private static final String INPUT_DATE_FORMAT = "dd-mm-yyyy";
	
	private static final String OUTPUT_DATE_FORMAT = "yyyy-mm-dd";

	@Override
	@PlusTransactional
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		try {
			specLabelPrefix.addAll(Arrays.asList("%T", "%N"));
			
			if (StringUtils.isNotEmpty(jobRun.getRtArgs())) { 
				dates = jobRun.getRtArgs().split(" ");
			}
			
			String fileName = getFileName();
			CSVWriter csvWriter = new CSVWriter(new FileWriter(fileName, true));
			writeHeader(csvWriter);
			
			int rowIdx = 0;
			for (String prefix : specLabelPrefix) {
				List<Object[]> results = executeQuery(prefix, parseInputDate(dates[0]), parseInputDate(dates[1]));
				writeToCsvFile(csvWriter, results);
				rowIdx += results.size();
			}
			
			csvWriter.writeNext(new String[]{System.getProperty("line.separator")});
			csvWriter.writeNext(new String[]{"Total number of incorrect record: " + rowIdx});
			csvWriter.close();
			
			jobRun.setLogFilePath(fileName);
			daoFactory.getScheduledJobDao().saveOrUpdateJobRun(jobRun);
		} catch (Exception e) {
			System.out.println("Error occurred while generating 'Incorrect Pathological-Status Report'");
			e.printStackTrace();
		}
	}
	
	private void writeHeader(CSVWriter csvWriter) throws ParseException {
		csvWriter.writeNext(new String[]{"Duration: " + parseInputDate(dates[0]) + " to " + parseInputDate(dates[1])});
		csvWriter.writeNext(new String[]{System.getProperty("line.separator")});
		csvWriter.writeNext(new String[]{"Report for Incorrect Pathological Status: "});
		csvWriter.writeNext(new String[]{System.getProperty("line.separator")});
		csvWriter.writeNext(new String[]{"CP Short Title", "TRID", "Specimen Label", "Pathological Status", "Tissue Site", "Tissue Side"});
	}

	private void writeToCsvFile(CSVWriter csvWriter, List<Object[]> results) throws IOException {
		results.forEach(row -> {
			String cp_short_title = getStringValue(row[0]);
			String trid = getStringValue(row[1]);
			String label = getStringValue(row[2]);
			String path_status = getStringValue(row[3]);
			String tissue_site = getStringValue(row[4]);
			String tissue_side = getStringValue(row[5]);
			
			csvWriter.writeNext(new String[]{cp_short_title, trid, label, path_status, tissue_site, tissue_side});
		});
		
		csvWriter.flush();
	}

	private String getFileName() {
		String location = cfgSvc.getDataDir() + "/os_report";
		File dir = new File(location);
		if (!dir.exists()) {
			dir.mkdir();
		}
		
		return location + "/Incorrect_Path_Status_" + Calendar.getInstance().getTimeInMillis() + ".csv";
	}

	private String parseInputDate(String date) throws ParseException {
		SimpleDateFormat inputFormat = new SimpleDateFormat(INPUT_DATE_FORMAT);
		SimpleDateFormat outputFormat = new SimpleDateFormat(OUTPUT_DATE_FORMAT);
		
		return outputFormat.format(inputFormat.parse(date));
	}
	
	@SuppressWarnings("unchecked")
	private List<Object[]> executeQuery(String prefix, String startDate, String endDate) 
			throws HibernateException, ParseException {
		return sessionFactory.getCurrentSession()
				.createSQLQuery(INCORRECT_PATH_STATUS_SPECIMEN)
				.setParameter(1, prefix)
				.setParameter(2, prefix)
				.setParameter(3, startDate)
				.setParameter(4, endDate)
				.list();
	}
	
	private String getStringValue(Object obj) { 
		return obj != null ? obj.toString() : null; 
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
            "      spec.activity_status != 'Disabled' and spec.lineage = 'New' and spec.label like ? " + 
            "    group by " +
            "      cp.short_title, visit.name, visit.identifier " + 
            "    having " +
            "      count(distinct spec.pathological_status) > 1 " + 
            "  ) vis on vis.identifier = spec.specimen_collection_group_id " + 
            "where " +
            "  spec.label like ? " +
            "  and vis.collection_timestamp between concat(?, ' 00:00:00') and concat(?, ' 23:59:59') " +
            "order by " + 
            "  spec.label ";
}