package com.krishagni.openspecimen.sgh.report.processor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;

import au.com.bytecode.opencsv.CSVWriter;

public abstract class CustomCsvReportProcessor {
	@Autowired
	private SessionFactory sessionFactory;
	
	@Autowired
	private ConfigurationService cfgSvc;
	
	@Autowired
	private DaoFactory daoFactory;

	private CSVWriter csvWriter;
	
	private Integer resultSize;

	public void process(ScheduledJobRun jobRun, String fileName, String dir, String[] header, Map<String, List<Map<String, Object>>> queriesAndParamsMap) throws IOException {
		String file = initFile(fileName, dir);
		initCsvWriter(file);
		csvWriter.writeNext(header);

		List<Object[]> resultSet = new ArrayList<>();

		queriesAndParamsMap.forEach((query,paramList) -> {
			paramList.forEach(param -> resultSet.addAll(executeQuery(query, param)));
		});

		for (Object[] rs : resultSet) {
			writeNextLine(rs);
			incrementResultSize();

			if (resultSize % 50 == 0) {
				flush();
			}
		}

		csvWriter.writeNext(new String[]{System.lineSeparator()});
		csvWriter.writeNext(new String[]{"Total number of incorrect record: " + getResultSize()});

		flush();
		closeWriter();
		jobRun.setLogFilePath(file);
		daoFactory.getScheduledJobDao().saveOrUpdateJobRun(jobRun);
	}

	private String initFile(String fileName, String dirName) {
		String dir = dirName != null ? (getDirPath(dirName)) : getDirPath();
		mkdirIfAbs(dir);

		return (dir + File.separator + fileName);
	}

	@SuppressWarnings("unchecked")
	private List<Object[]> executeQuery(String query, Map<String, Object> params) {
		Query qry = sessionFactory.getCurrentSession().createSQLQuery(query);

		params.forEach((k,v) -> {
			qry.setParameter(k, v);
		});

		return qry.list();
	}

	private void writeNextLine(Object[] rowData) {
		List<String> data = Arrays.stream(rowData).map(String::valueOf).collect(Collectors.toList());
		csvWriter.writeNext(data.toArray(new String[0]));
	}

	private void closeWriter() {
		IOUtils.closeQuietly(csvWriter);
	}

	private void initCsvWriter(String file) throws IOException {
		this.resultSize = 0;
		csvWriter = new CSVWriter(new FileWriter(file, true));
	}

	private Integer getResultSize() {
		return resultSize;
	}

	private void incrementResultSize() {
		this.resultSize++;
	}

	private void flush() throws IOException {
		csvWriter.flush();
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
}
