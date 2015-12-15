package com.krishagni.openspecimen.sgh.reports;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

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
		
	@PlusTransactional
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
		File report = new File(fileName);
		FileWriter fw;
		int counter = 0;
		try {
			fw = new FileWriter(report);
			fw.write("Duration: " + startDate + " to " + endDate + lineSeperator+lineSeperator);
			fw.write("Report for Incorrect Tissue Site and Side: " + lineSeperator+lineSeperator);
			fw.write("TRID,Specimen Label,Tissue Site,Tissue Side" + lineSeperator);
			
			for (Object[] object : list) {
				String trid = (object[0] != null) ? object[0].toString() : null;
				String label = (object[1] != null) ? object[1].toString() : null;
				String tissue_site = (object[2] != null) ? object[2].toString() : null;
				String tissue_side = (object[3] != null) ? object[3].toString() : null;
				fw.write("\"" + trid + "\",\"" + label + "\",\"" + tissue_site + "\",\"" + tissue_side + "\"");
				fw.write(lineSeperator);
				counter++;
			}
			fw.write(lineSeperator + "Total number of incorrect record," + counter + lineSeperator);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		jobRun.setLogFilePath(fileName);
		daoFactory.getScheduledJobDao().saveOrUpdateJobRun(jobRun);
	}

	@Override
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		String[] dates = {"", ""};
		if(jobRun.getRtArgs() != null && !jobRun.getRtArgs().isEmpty()) {
			dates = jobRun.getRtArgs().split(" ");
		}

		startDate = getDate(dates[0]);
		endDate = getDate(dates[1]);

		String query = "select\r\n"
			+ "scg.name as TRID,\r\n" 
			+ "spmn.label as 'Specimen Label',\r\n"
			+ "spmn.tissue_site as 'Tissue Site',\r\n"
			+ "spmn.tissue_side as 'Tissue Side'\r\n"
			+ "from\r\n" 
			+ "catissue_specimen spmn\r\n" 
			+ "join (select\r\n"
			+ "scg.identifier,\r\n" 
			+ "scg.collection_timestamp,\r\n"
			+ "scg.name \r\n"
			+ "from catissue_specimen_coll_group scg\r\n" 
			+ "join catissue_specimen spmn on spmn.specimen_collection_group_id = scg.identifier\r\n" 
			+ "where\r\n"
			+ "spmn.activity_status in ('Active','Closed')\r\n" 
			+ "and spmn.collection_status='Collected'\r\n" 
			+ "and spmn.lineage='New'\r\n" 
			+ "group by scg.name\r\n"
			+ "having count(distinct spmn.tissue_site)>1 or\r\n" 
			+ "count(distinct spmn.tissue_side)>1) as scg on spmn.specimen_collection_group_id= scg.identifier\r\n"
			+ "where spmn.lineage = 'New'\r\n" 
			+ "and spmn.label is not null\r\n"
			+ "and scg.collection_timestamp between concat( '"
			+ startDate + "' , ' 00:00:00' ) AND CONCAT( '" + endDate + "' , ' 23:59:59' )\r\n"
			+ "order by spmn.label\r\n";

		executeQuery(query, jobRun);
	}

	private String getDate(String date){
		String [] ddmmyyyy = date.split("-");
		String yyyymmdd = ddmmyyyy[2] + "-"+ddmmyyyy[1] + "-"+ddmmyyyy[0];
		return yyyymmdd;
	}
}
