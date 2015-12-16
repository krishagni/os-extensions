package com.krishagni.openspecimen.sgh.reports;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;

@Configurable
public class IncorrectTissueSiteReport implements ScheduledTask {
	@Autowired
	private SessionFactory sessionFactory;
	
	@Autowired
	private DaoFactory daoFactory;
	
	@Autowired
	private ConfigurationService cfgSvc;

	private String startDate;
	
	private String endDate;
	
	@SuppressWarnings("unchecked")
	private void executeQuery(String query, ScheduledJobRun jobRun) {
		List<Object[]> list = sessionFactory.getCurrentSession().createSQLQuery(query).list();
		String lineSeperator = System.getProperty( "line.separator" );
		String location = cfgSvc.getDataDir() + "/os_report";
		File dir = new File(location);
		if(!dir.exists()) {
			dir.mkdir();
		}
		
		String fileName = location + "/Incorrect_tissue_site_and_side_" + Calendar.getInstance().getTimeInMillis() + ".csv";
	        
		int counter = 0;
		try {
			CSVWriter csvWriter = new CSVWriter(new FileWriter(fileName, true));
			csvWriter.writeNext(new String[]{"Duration: " + startDate + " to " + endDate});
			csvWriter.writeNext(new String[]{lineSeperator});
			csvWriter.writeNext(new String[]{"Report for Incorrect Tissue Site and Side : "});
			csvWriter.writeNext(new String[]{lineSeperator});
			csvWriter.writeNext(new String[]{"TRID", "Specimen Label", "Tissue Site", "Tissue Side"});
			
			for (Object[] object : list) {
				String trid = (object[0] != null) ? object[0].toString() : null;
				String label = (object[1] != null) ? object[1].toString() : null;
				String tissue_site = (object[2] != null) ? object[2].toString() : null;
				String tissue_side = (object[3] != null) ? object[3].toString() : null;
				csvWriter.writeNext(new String[]{trid, label, tissue_site, tissue_side});
				counter++;
			}
			
			csvWriter.writeNext(new String[]{lineSeperator});
			csvWriter.writeNext(new String[]{"Total number of incorrect record," + counter});
			csvWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		jobRun.setLogFilePath(fileName);
		daoFactory.getScheduledJobDao().saveOrUpdateJobRun(jobRun);
	}
	
	@Override
	@PlusTransactional
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		String[] dates = {"", ""};
		if(jobRun.getRtArgs() != null && !jobRun.getRtArgs().isEmpty()) {
			dates = jobRun.getRtArgs().split(" ");
		}
		
		startDate = getDate(dates[0]);
		endDate = getDate(dates[1]);
		String query = String.format(QUERY, startDate , endDate);
		executeQuery(query, jobRun);
	}


	private String getDate(String date){
		String [] ddmmyyyy = date.split("-");
		String yyyymmdd = ddmmyyyy[2] + "-"+ddmmyyyy[1] + "-"+ddmmyyyy[0];
		return yyyymmdd;
	}

	private static final String QUERY =
			"select " +
			"  s.name as trid, spmn.label as specimen_label, " +
			"  spmn.tissue_site as tissue_site, spmn.tissue_side as tissue_side " +
			"from " +
			"  catissue_specimen spmn " +
			"  join (" +
			"    select " +
			"      scg.identifier, scg.collection_timestamp, scg.name " +
			"    from " +
			"      catissue_specimen_coll_group scg " +
			"      join catissue_specimen spec on spec.specimen_collection_group_id = scg.identifier " +
			"    where " +
			"      spec.activity_status in ('Active', 'Closed') and " + 
			"      spec.collection_status = 'Collected' and " + 
			"      spec.lineage = 'New' " +
			"    group by " + 
			"      scg.name " +
			"    having " +
			"      count(distinct spec.tissue_site) > 1 or " +
			"      count(distinct spec.tissue_side) > 1 " + 
			"  ) as s on spmn.specimen_collection_group_id = s.identifier " +
			"where " + 
			"  spmn.lineage = 'New' and " +
			"  spmn.label is not null and " +
			"  s.collection_timestamp between concat( '%s' , ' 00:00:00' ) and concat( '%s' , ' 23:59:59' ) " + 
			"order by " +
			"  spmn.label"; 

}
