package com.krishagni.openspecimen.msk.ppbc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.SftpUtil;
import com.krishagni.catissueplus.core.common.util.SshSession;

public class ExportJobDriver implements ScheduledTask {
	private static final Log logger = LogFactory.getLog(ExportJobDriver.class);
	
	private static final String[] csvFilenames = new String[] {
			"Accession.csv", 
			"Specimen_Request.csv", 
			"Distribution.csv", 
			"Details.csv", 
			"Specimen_Request_Details.csv"
	};
	
	private ScheduledTask[] tasks = {
			   new DistributionProtocolExport(),
			   new ParticipantExport()
			};
	
	
	@Override
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		getExportFolder().mkdir();
		
		for (ScheduledTask task : tasks) {
			task.doJob(jobRun);
		}
		
		ensureFolderIsAccessible();
		loadToDatabase();
		cleanUpTempFiles();
	}

	private void loadToDatabase() {
		for (String file : csvFilenames) {
			loadToDatabase(file);
		}
	}

	private void loadToDatabase(String filename) {
		switch (filename) {
			case "Distribution.csv":
				executeLoadDataQuery(distributionSqlQuery);
				break;
			
			case "Accession.csv":
				executeLoadDataQuery(accessionSqlQuery);
				break;
			
			case "Details.csv":
				executeLoadDataQuery(detailsSqlQuery);
				break;
				
			case "Specimen_Request.csv":
				executeLoadDataQuery(specimenRequestSqlQuery);
				break;
				
			case "Specimen_Request_Details.csv":
				executeLoadDataQuery(specimenRequestDetailsSqlQuery);
				break;
				
			default:
				logger.error("Cannot load into DB - Unknown file: " + filename);
		}
	}

	private void executeLoadDataQuery(String query) {
		SingleConnectionDataSource scds = new SingleConnectionDataSource("jdbc:mysql://localhost:3306/loadfromcsv","swapnil","root", true);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(scds);
		jdbcTemplate.execute(query);
		
		scds.destroy();
	}
	
	private void ensureFolderIsAccessible()  {
		File destination = new File(dbDataDir);
		File source = getExportFolder();
		
		if (StringUtils.isEmpty(remoteHost) || StringUtils.isEmpty(remoteUsername) || StringUtils.isEmpty(remotePassword)) {
		    	FileUtils.listFiles(source, null, false).forEach(
		    			file -> {
							try {
								FileUtils.copyFileToDirectory(file, destination);
							} catch (IOException e) {
								logger.error("Error while copying csv file from source to destination directory", e);
							}
						}
		    	);
		} else {
			// IoUtil.zip
		    putFileOnRemote(source.getAbsolutePath(), destination.getAbsolutePath());
		}
	}
	
	private void putFileOnRemote(String localPath, String remotePath) {
		SshSession ssh = new SshSession(remoteHost, remoteUsername, remotePassword);
		ssh.connect();

		SftpUtil sftp = ssh.newSftp();
		sftp.put(localPath, remotePath);

		sftp.close();
		ssh.close();
	}
	
	private void cleanUpTempFiles() {
		try {
			// Zip the OSDataDir/export folder
			FileUtils.deleteDirectory(getExportFolder());
			
			if (StringUtils.isEmpty(remoteHost) || StringUtils.isEmpty(remoteUsername) || StringUtils.isEmpty(remotePassword)) {
				for (String file : csvFilenames) {
					FileUtils.deleteQuietly(new File(dbDataDir, file));
				}
			}
			
		} catch (IOException e) {
			logger.error("Error cleaning up temporary files", e);
		}
	}
	
	private File getExportFolder() {
		String folderName = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
		return new File(ConfigUtil.getInstance().getDataDir(), folderName);
	}
	
	private final static String dbDataDir = "/usr/local/var/mysql";
	private final static String remoteHost = "";
	private final static String remoteUsername = "";
	private final static String remotePassword = "";
	private final static String specimenRequestSqlQuery = "LOAD DATA INFILE '/usr/local/var/mysql/Specimen_Request.csv'\n" + 
			"IGNORE INTO TABLE Specimen_Request\n" + 
			"FIELDS TERMINATED BY ',' ENCLOSED BY '\"'\n" + 
			"LINES TERMINATED BY '\\n'\n" + 
			"IGNORE 1 LINES\n" + 
			"(@col1, @col2, @col3, @col4, @col5, @col6, @col7, @col8, @col9, @col10,\n" + 
			"@col11, @col12, @col13, @col14, @col15, @col16, @col17, @col18, @col19,\n" + 
			"@col20, @col21, @col22, @col23, @col24, @col25, @col26, @col27, @col28)\n" + 
			"SET\n" + 
			"TBR_REQUEST_TITLE = @col1,\n" + 
			"TBR_SOURCE_REQUEST = @col2,\n" + 
			"TBR_INSTITUTE_DESC = @col3,\n" + 
			"TBR_DEPT_DESC = @col4,\n" + 
			"TBR_REQUESTER_DESC = @col5,\n" + 
			"TBR_REQUEST_DT = STR_TO_DATE(@col6, '%b %d, %Y %H:%i'),\n" + 
			"TBR_ILAB_NO = @col7,\n" + 
			"TBR_FINALIZE_FLAG = @col8,\n" + 
			"TBR_COST_CENTER = @col9,\n" + 
			"TBR_FUND_ID = @col10,\n" + 
			"TBR_MTA_FLAG = @col11,\n" + 
			"TBR_DISTRIBUTION_OPTION_DESC = @col12,\n" + 
			"TBR_HBC_ID = @col13,\n" + 
			"TBR_HBC_COMMITTEE_APPROVAL_DT = STR_TO_DATE(@col14, '%b %d, %Y %H:%i'),\n" + 
			"TBR_MIN_UNIT = @col15,\n" + 
			"TBR_MTA_APPROVAL_DT = STR_TO_DATE(@col16, '%b %d, %Y %H:%i'),\n" + 
			"TBR_PICKUP_ARRANGEMENT_DESC = @col17,\n" + 
			"TBR_PROSPECT_FLAG = @col18,\n" + 
			"TBR_TYPE_DESC = @col19,\n" + 
			"TBR_RESTROSPECT_FLAG = @col20,\n" + 
			"TBR_SPECIMEN_COLLECTION_METHOD = @col21,\n" + 
			"TBR_COMMENTS = @col22,\n" + 
			"TBR_CONTACT_NAME = @col23,\n" + 
			"TBR_SPECIMEN_USAGE_DESC = @col24,\n" + 
			"TBR_WAIVER_NO = @col25,\n" + 
			"TBR_MIN_SIZE_DESC = @col26,\n" + 
			"TBR_STS_DESC = @col27,\n" + 
			"TBR_SPECIAL_HANDLING_DESC = @col28;";
	
	private final static String distributionSqlQuery = "LOAD DATA INFILE '/usr/local/var/mysql/Distribution.csv'\n" + 
			"IGNORE INTO TABLE Distribution\n" + 
			"FIELDS TERMINATED BY ',' ENCLOSED BY '\"'\n" + 
			"LINES TERMINATED BY '\\n'\n" + 
			"IGNORE 1 LINES\n" + 
			"(@col1, @col2, @col3, @col4, @col5, @col6)\n" + 
			"SET \n" + 
			"TBDS_SPECIMEN_REQUEST_ID = @col1,\n" + 
			"TBDS_DISTRIBUTION_DT = @col2,\n" + 
			"TBDS_SOURCE_REQUEST = @col3,\n" + 
			"TBDS_BILLING_AMT = @col4,\n" + 
			"SPECIMEN_LABEL = @col5,\n" + 
			"TBDS_BILLING_DT = STR_TO_DATE(@col6, '%b %d, %Y %H:%i');";
	
	private final static String specimenRequestDetailsSqlQuery = "LOAD DATA INFILE '/usr/local/var/mysql/Specimen_Request_Details.csv'\n" + 
			"IGNORE INTO TABLE SPECIMEN_REQUEST_DETAILS\n" + 
			"FIELDS TERMINATED BY ',' ENCLOSED BY '\"'\n" + 
			"LINES TERMINATED BY '\\n'\n" + 
			"IGNORE 1 LINES\n" + 
			"(@col1, @col2, @col3, @col4, @col5, \n" + 
			" @col6, @col7, @col8, @col9, @col10,\n" + 
			" @col11, @col12, @col13, @col14, @col15)\n" + 
			"SET\n" + 
			"TBRD_SPECIMEN_TYPE_CD = @col1,\n" + 
			"TBRD_SITE_DESC = @col2,\n" + 
			"TBRD_SUB_SITE_DESC = @col3,\n" + 
			"TBRD_SUB2_SITE_DESC = @col4,\n" + 
			"TBRD_CATEGORY_DESC = @col5,\n" + 
			"TBRD_EXPECTED_AMT = @col6,\n" + 
			"TBRD_BILLING_AMT = @col7,\n" + 
			"TBRD_SOURCE_REQUEST = @col8,\n" + 
			"TBRD_HISTOLOGY_DESC = @col9,\n" + 
			"TBRD_HISTOLOGY_SUB_DESC = @col10,\n" + 
			"TBRD_HISTOLOGY_SUB2_DESC = @col11,\n" + 
			"TBRD_HISTOLOGY_SUB3_DESC = @col12,\n" + 
			"TBRD_QUALITY_DESC = @col13,\n" + 
			"TBRD_UNIT_DESC = @col14,\n" + 
			"TBRD_NOTES = @col15;";
	
	private final static String accessionSqlQuery = "LOAD DATA INFILE '/usr/local/var/mysql/Accession.csv'\n" + 
			"IGNORE INTO TABLE ACCESSION\n" + 
			"FIELDS TERMINATED BY ',' ENCLOSED BY '\"'\n" + 
			"LINES TERMINATED BY '\\n'\n" + 
			"IGNORE 1 LINES\n" + 
			"(@col1, @col2, @col3, @col4, @col5, @col6, @col7, @col8, @col9, @col10,\n" + 
			"@col11, @col12, @col13, @col14, @col15, @col16, @col17, @col18, @col19,\n" + 
			"@col20, @col21, @col22, @col23, @col24, @col25, @col26, @col27, @col28,\n" + 
			"@col29, @col30, @col31, @col32, @col33, @col34, @col35, @col36, @col37,\n" + 
			"@col38,@col39)\n" + 
			"SET\n" + 
			"TBA_CRDB_MRN = @col1,\n" + 
			"TBA_PT_DEIDENTIFICATION_ID = @col2,\n" + 
			"TBD_BANK_NUM = @col3,\n" + 
			"TBA_PROCUREMENT_DTE = @col4,\n" + 
			"TBD_BANK_SUB_CD = @col5, \n" + 
			"TBA_DISEASE_DESC = @col6,\n" + 
			"TBA_ACCESSION_NUM = @col7,\n" + 
			"TBD_BANK_NOTE = @col8,\n" + 
			"TBA_DIAGNOSIS_NOTE = @col9,\n" + 
			"TBA_SURG_STRT_DT = STR_TO_DATE(@col10, '%b %d, %Y %H:%i'),\n" + 
			"TBA_PATH_REVIEW_DT = STR_TO_DATE(@col11, '%b %d, %Y %H:%i'),\n" + 
			"TBA_SURGEON_NAME = @col12,\n" + 
			"SURGICAL_PATH_REPORT = @col13,\n" + 
			"TBD_NUN_N = @col14,\n" + 
			"TBD_NUN_T = @col15,\n" + 
			"TBD_OCT_N = @col16,\n" + 
			"TBD_OCT_T = @col17,\n" + 
			"PARENT_SPECIMEN_LABEL = @col18,\n" + 
			"TBD_SPECIMEN_TYPE_DESC = @col19,\n" + 
			"TBA_SITE_DESC = @col20,\n" + 
			"TBA_SUB_SITE_DESC = @col21,\n" + 
			"TBA_SUB2_SITE_DESC = @col22,\n" + 
			"TBA_SITE_SIDE_DESC = @col23,\n" + 
			"TBA_TISSUE_TYPE_DESC = @col24,\n" + 
			"TBA_SITE_TEXT = @col25,\n" + 
			"TBA_RESECT_DT = @col26,\n" + 
			"TBA_BIOBANK_RECEIPT_DT = @col27,\n" + 
			"TBA_PART_NUM = @col28,\n" + 
			"TBA_SUB_PART_NUM = @col29,\n" + 
			"TBA_BIOBANK_TECH_NAME = @col30,\n" + 
			"TBA_TEMPERATURE_COND_DESC = @col31,\n" + 
			"TBA_BIOBANK_TEMPERATURE_COND_DESC = @col32,\n" + 
			"TBA_SITE_LOCATION_DESC = @col33,\n" + 
			"TBA_ACCESSION_RECEIPT_DT = STR_TO_DATE(@col34, '%b %d, %Y %H:%i'),\n" + 
			"TBA_HISTOLOGY_DESC = @col35,\n" + 
			"TBA_HISTOLOGY_SUB_DESC = @col36,\n" + 
			"TBA_HISTOLOGY_SUB2_DESC = @col37,\n" + 
			"TBA_HISTOLOGY_SUB3_DESC = @col38,\n" + 
			"TBA_HARVEST_PA_NAME = @col39;";
	
	private final static String detailsSqlQuery = "LOAD DATA INFILE '/usr/local/var/mysql/details.csv'\n" + 
			"IGNORE INTO TABLE DETAILS\n" + 
			"FIELDS TERMINATED BY ',' ENCLOSED BY '\"'\n" + 
			"LINES TERMINATED BY '\\n'\n" + 
			"IGNORE 1 LINES\n" + 
			"(@col1, @col2, @col3, @col4, @col5, @col6, @col7, @col8, @col9, @col10,\n" + 
			"@col11, @col12, @col13, @col14, @col15, @col16, @col17, @col18)\n" + 
			"SET\n" + 
			"PARENT_SPECIMEN_LABEL = @col1,\n" + 
			"ALIQUOT_LABEL = @col2,\n" + 
			"TBD_CATEGORY_DESC = @col3,\n" + 
			"TBD_SPECIMEN_TYPE_DESC = @col4,\n" + 
			"TBD_BANK_SEQ_NUM = @col5,\n" + 
			"TBD_VOL = @col6,\n" + 
			"TBD_WEIGHT = @col7,\n" + 
			"TBD_SAMPLE_PROCESS_DT = STR_TO_DATE(@col8, '%b %d, %Y %H:%i'),\n" + 
			"TBD_QUALITY_DESC = @col9,\n" + 
			"TBD_TIME_LAPSE_MIN = @col10,\n" + 
			"TBD_UNIT_DESC = @col11,\n" + 
			"TBD_SPECIAL_HANDLING_DESC = @col12,\n" + 
			"TBD_STERILE_CODE_DESC = @col13,\n" + 
			"TBD_BIOBANK_TECH_NAME = @col14,\n" + 
			"TBD_ADDTL_DETAILS = @col15,\n" + 
			"TBD_ADDTL_PROCESS_DT = STR_TO_DATE(@col16, '%b %d, %Y %H:%i'),\n" + 
			"TBD_ADDTL_PROCESS_TECH_NAME = @col17,\n" + 
			"TBD_ADDTL_PROCESS_TEMPERATURE_DESC = @col18;";
}
