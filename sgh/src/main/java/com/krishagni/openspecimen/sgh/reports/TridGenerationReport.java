package com.krishagni.openspecimen.sgh.reports;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;

@Configurable
public class TridGenerationReport implements ScheduledTask {
	
	@Autowired
	private SessionFactory sessionFactory;
	
	@Autowired
	private DaoFactory daoFactory;
	
	@Autowired
	private ConfigurationService cfgSvc;

	private String startDate;
	
	private String endDate;

	@Override
	@PlusTransactional
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		String[] dates = {"", ""};
		if(jobRun.getRtArgs() != null && !jobRun.getRtArgs().isEmpty()) { 
			dates = jobRun.getRtArgs().split(" ");
		}

		startDate = getDate(dates[0]);
		endDate = getDate(dates[1]);
		String query =
				"select " +
				"  job_item.item_label, concat(u.first_name,' ', u.last_name), job.submission_date " +
				"from " +
				"  os_label_print_jobs job " +
				"  join os_label_print_job_items job_item on job_item.job_id = job.identifier " +
				"  join catissue_user u on u.identifier = job.submitted_by " +
				"where " +
				"  job.submission_date between '" + startDate + " 00:00:00' and '" + endDate + " 23:59:59' and job_item.item_label like 'z%'";
		executeQuery(query, jobRun);
	}

	@SuppressWarnings("unchecked")
	private void executeQuery(String query, ScheduledJobRun jobRun) {
		String lineSeperator = System.getProperty( "line.separator" );
		String location = cfgSvc.getDataDir() + "/os_report";
		File dir = new File(location);
		if(!dir.exists()) {
			dir.mkdir();
		}
		
		String fileName = location + "/Generated_TRIDs_Report" + Calendar.getInstance().getTimeInMillis() + ".csv";
	        
		int counter = 0;
		try {
			CSVWriter csvWriter = new CSVWriter(new FileWriter(fileName, true));
			csvWriter.writeNext(new String[]{"Duration: " + startDate + " to " + endDate});
			csvWriter.writeNext(new String[]{lineSeperator});
			csvWriter.writeNext(new String[]{"Report for Generated TRIDs : "});
			csvWriter.writeNext(new String[]{lineSeperator});
			csvWriter.writeNext(new String[]{"TRID", "User Name", "Generation Date"});
			List<Object[]> list = sessionFactory.getCurrentSession().createSQLQuery(query).list();

			for (Object[] object : list) {
				String trid = getStringValue(object[0]);
				String userName = getStringValue(object[1]);
				String date = getStringValue(object[2]);
				if(StringUtils.isNotBlank(trid) && !trid.contains("_") && !trid.contains(" ")) {
					csvWriter.writeNext(new String[]{trid, userName, date});
					counter++;
				}
			}
			
			csvWriter.writeNext(new String[]{lineSeperator});
			csvWriter.writeNext(new String[]{"Total records: ," + counter});
			csvWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		jobRun.setLogFilePath(fileName);
		daoFactory.getScheduledJobDao().saveOrUpdateJobRun(jobRun);
	}
	
	private String getDate(String date) throws ParseException {
		SimpleDateFormat parser = new SimpleDateFormat("dd-mm-yyyy");
		Date dt = parser.parse(date);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd");
		return formatter.format(dt);
	}

	private String getStringValue(Object obj) { 
		return obj != null ? obj.toString() : null; 
	}       

//	private static final String QUERY =
//			"select " +
//			"  job_item.item_label, concat(u.first_name,' ', u.last_name), job.submission_date " +
//			"from " +
//			"  os_label_print_jobs job " +
//			"  join os_label_print_job_items job_item on job_item.job_id = job.identifier " +
//			"  join catissue_user u on u.identifier = job.submitted_by " +
//			"where " +
//			"  job.submission_date between concat( '%s' , ' 00:00:00' ) and concat( '%s' , ' 23:59:59' ) and job_item.item_label like 'z%'";
}
