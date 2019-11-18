package com.krishagni.openspecimen.staging.importer;

import java.io.File;
import java.io.IOException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SourceDbManager {
	private SingleConnectionDataSource dataSource;

	private JdbcTemplate jdbcTemplate;

	private SourceDbInfo sourceDbInfo;

	public SingleConnectionDataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(SingleConnectionDataSource dataSource) {
		this.dataSource = dataSource;
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public SourceDbInfo getSourceDbInfo() {
		return sourceDbInfo;
	}

	public void setSourceDbInfo(SourceDbInfo sourceDbInfo) {
		this.sourceDbInfo = sourceDbInfo;
	}

	public String getSql() {
		return sourceDbInfo.getSql();
	}

	public String getSource() {
		return sourceDbInfo.getSource();
	}

	public void formatSql(String... args) {
		String[] arr = new String[args.length + 1];
		System.arraycopy(args, 0, arr, 0, args.length);

		sourceDbInfo.setSql(String.format(getSql(), (Object[]) arr));
	}

	public void connect(String url, String user, String pwd) {
		dataSource = new SingleConnectionDataSource(url, user, pwd, false);
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void loadJson(String file) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		sourceDbInfo = mapper.readValue(new File(file), SourceDbInfo.class);
	}

	public void closeConnection() {
		dataSource.destroy();
	}
}
