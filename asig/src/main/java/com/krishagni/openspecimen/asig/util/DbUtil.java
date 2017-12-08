package com.krishagni.openspecimen.asig.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class DbUtil {
	private static final Logger logger = Logger.getLogger(DbUtil.class);

	public static synchronized DataSource getDataSource(Properties props)
		throws Exception{

		DriverManagerDataSource ds = new DriverManagerDataSource();
		String driverClass = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		ds.setDriverClassName(driverClass);
		//ds.setUsername(props.getProperty("database.username"));
		//ds.setPassword(props.getProperty("database.password"));

		String jdbcUrl = "jdbc:sqlserver://" + props.getProperty("database.host") + ":"
			+ props.getProperty("database.port") + ";"
			+ "databaseName="+ props.getProperty("database.name") +";integratedSecurity=true;";

		ds.setUrl(jdbcUrl);

		logger.info("*********************************************************");
		logger.info("Connection String = " + jdbcUrl);
		logger.info("*********************************************************");

		return ds;
	}
}
