package com.krishagni.openspecimen.sgh.report.processor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;

public abstract class CustomCsvReportProcessor {
	@Autowired
	private SessionFactory sessionFactory;
	
	public void process(ScheduledJobRun jobRun) throws Exception {
		String filePath = getReportPath();
		CsvWriter writer = CsvFileWriter.createCsvFileWriter(new File(filePath));

		try {
			writer.writeNext(getHeader());
			int rowCount = 0;

			Map<String, List<Map<String, Object>>> queries = getQueries();

			for (Entry<String, List<Map<String, Object>>> entry : queries.entrySet()) {
				for (Map<String, Object> params : entry.getValue()) {
					List<Object[]> rows = executeQuery(entry.getKey(), params);
					rowCount += rows.size();

					writeToCsv(writer, rows);
				}
				writer.flush();
			}

			postProcess(writer, rowCount);

			writer.flush();
			jobRun.setLogFilePath(filePath);
		} finally {
			writer.close();
		}
	}

	public abstract String getReportPath();

	public abstract String[] getHeader();

	public abstract Map<String, List<Map<String, Object>>> getQueries();

	public abstract void postProcess(CsvWriter writer, int totalRows);

	private void writeToCsv(CsvWriter writer, List<Object[]> rows) throws IOException {
		int rowCount = 0;

		for (Object[] row : rows) {
			writeNextLine(writer, row);

			++rowCount;
			if (rowCount % 50 == 0) {
				writer.flush();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private List<Object[]> executeQuery(String query, Map<String, Object> params) {
		Query qry = sessionFactory.getCurrentSession().createSQLQuery(query);

		params.forEach((k,v) -> {
			qry.setParameter(k, v);
		});

		return qry.list();
	}
	
	private void writeNextLine(CsvWriter writer,Object[] rowData) {
		List<String> data = Arrays.stream(rowData).map(String::valueOf).collect(Collectors.toList());
		writer.writeNext(data.toArray(new String[0]));
	}
}
