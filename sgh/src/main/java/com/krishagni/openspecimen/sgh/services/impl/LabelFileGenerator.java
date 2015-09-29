
package com.krishagni.openspecimen.sgh.services.impl;

import java.io.BufferedWriter;

import org.springframework.beans.factory.annotation.Configurable;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;

@Configurable
public class LabelFileGenerator implements ScheduledTask{

	private static Connection connection = null;
	
	private static Properties properties = null;

	private static Log LOGGER = LogFactory.getLog(LabelFileGenerator.class);
	
	@Override
	public void doJob(ScheduledJobRun jobRun) 
	throws Exception {
		properties = Utility.getProperties();
		connection = Utility.getConnection(properties);
		
		try {
			PreparedStatement preparedStatement = connection.prepareStatement(GET_PRINT_DATA_JSON);
			ResultSet result = preparedStatement.executeQuery();
			List<Long> jobIds = new ArrayList<Long>();
			while(result.next()) {
				Long jobId = Long.valueOf(result.getLong(1));
				String printerName = result.getString(2);
				String labelType = result.getString(3);
				String data = result.getString(4);
				
				boolean cmdPrintStatus = generateLabelFile(jobId, printerName, labelType, data);
				if(cmdPrintStatus){
					jobIds.add(jobId);
				}
				
			}
			
			for (Long jobId : jobIds) {
				PreparedStatement preparedStmt = connection.prepareStatement(UPDATE_PRINT_STATUS);
				preparedStmt.setLong(1, jobId);
				preparedStmt.executeUpdate();
			}
			
		} catch (SQLException e) {
			LOGGER.error("Error while exporting the records from database.", e);
		}
	}
	
	private static boolean generateLabelFile(Long jobId, String printerName, String labelType, String data) throws Exception {
		String fileName = "";
		if (printerName != null && !printerName.isEmpty() && !printerName.equals("default")){
			fileName = "fil"+String.valueOf(jobId) + "." + printerName;
		} else {
			fileName = "fil"+String.valueOf(jobId);
		}
		File dir = new File(String.valueOf(properties.get("outputDir")));
		File actualFile = new File(dir, fileName + ".cmd");
		FileWriter fw = null;
		BufferedWriter bw = null;
		try
		{
			
			fw = new FileWriter(actualFile.getAbsolutePath());
			bw = new BufferedWriter(fw);
			
			JsonParser parser = new JsonParser();
			JsonObject jsonObj = (JsonObject)parser.parse(data);
			if (printerName != null && !printerName.isEmpty()){
				bw.write("\""+jsonObj.get("Specimen Label") + "\"" + ", " + "\"" + jsonObj.get("Name") + "\"");
			} else {
				bw.write(jsonObj.get("Specimen Label") +", "+ jsonObj.get("Name"));
			}
		}
		
		catch(Exception ex)
		{
			LOGGER.error("Error while parsing data", ex);
			throw ex;
		}
		finally
		{
			bw.close();
			fw.close();
		}
		return true;
	}

	private static final String UPDATE_PRINT_STATUS = 
			"update os_label_print_job_items set status = 'PRINTED' where identifier = ?";
	
	private static final String GET_PRINT_DATA_JSON = 
			"select identifier, printer_name, label_type, data from os_label_print_job_items where status like 'QUEUED'";
}
