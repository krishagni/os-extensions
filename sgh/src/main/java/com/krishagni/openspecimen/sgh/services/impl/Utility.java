package com.krishagni.openspecimen.sgh.services.impl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class Utility {

	private static Log LOGGER = LogFactory.getLog(Utility.class);

	public static Properties getProperties() {
		FileInputStream localFileInputStream;
		Properties properties = new Properties();
		try {
			localFileInputStream = new FileInputStream("os-db.Properties");

			properties = new Properties();
			properties.load(localFileInputStream);
			localFileInputStream.close();
		}
		catch (FileNotFoundException e) {
			LOGGER.error("Error while reading os-db.properties file", e);
		}
		catch (IOException e) {
			LOGGER.error("Error while reading os-db.properties file", e);
		}

		return properties;
	}

	public static Connection getConnection(Properties properties) {
		Connection connection = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");

			String connectionString = "jdbc:mysql://" + (String) properties.get("database.host") + ":"
					+ (String) properties.get("database.port") + "/" + (String) properties.get("database.name");

			connection = (Connection) DriverManager.getConnection(connectionString,
					(String) properties.get("database.username"), (String) properties.get("database.password"));

		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}

		return connection;
	}

	public static void closeConnection(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
			}
			catch (SQLException e) {
				LOGGER.error("Error while closing connection, no file generated", e);
			}
		}
	}

}