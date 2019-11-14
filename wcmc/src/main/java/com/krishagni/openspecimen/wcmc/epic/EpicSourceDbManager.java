package com.krishagni.openspecimen.wcmc.epic;

import java.io.File;
import java.io.IOException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EpicSourceDbManager {
	private SingleConnectionDataSource dataSource;

	private JdbcTemplate jdbcTemplate;
	
	private EpicSourceDbInfo epicSourceDbInfo;
	
	public void connect(String url, String user, String pwd) {
		dataSource = new SingleConnectionDataSource(url, user, pwd, false);
		jdbcTemplate = new JdbcTemplate(dataSource);
	}
	
	public void loadJson(String file) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		epicSourceDbInfo = mapper.readValue(new File(file), EpicSourceDbInfo.class);
	}
}
