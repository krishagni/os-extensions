package com.krishagni.openspecimen.sgh.report.processor;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;

public abstract class CustomCsvReportProcessor {
	@Autowired
	private SessionFactory sessionFactory;
	
	@Autowired
	private ConfigurationService cfgSvc;
	
	private static final String SG_REPORT_DIR = "os_report";
	
	public String getRtArgs(ScheduledJobRun jobRun) {
		String rtArgs = jobRun.getRtArgs();
		if (rtArgs != null && !rtArgs.isEmpty()) {
			return rtArgs;
		}
		return null;
	}
	
	public String initFile(String fileName) {
		return initFile(fileName, null);
	}
	
	public String initFile(String fileName, String dirName) {
		String dir = dirName != null ? (getDirPath(dirName)) : getDirPath();
		mkdirIfAbs(dir);
		
		return (dir + File.separator + fileName);
	}
	
	private String getDirPath() {
		return cfgSvc.getDataDir();
	}

	private String getDirPath(String dirName) {
		return cfgSvc.getDataDir() + File.separator + dirName;
	}

	private void mkdirIfAbs(String dirName) {
		File dir = new File(dirName);
		if (!dir.exists()) {
			dir.mkdir();
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<Object[]> executeQuery(String query, Map<String, Object> params) {
		Query qry = sessionFactory.getCurrentSession().createSQLQuery(query);
		
		params.forEach((k,v) -> {
			qry.setParameter(k, v);
		});
		
		return qry.list();
	}
}
