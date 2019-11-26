package com.openspecimen.ext.participant.source.impl;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.events.StagedParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.PvAttributes;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.openspecimen.ext.participant.error.ExtPartImpErrorCode;
import com.openspecimen.ext.participant.source.ExternalParticipantSource;

public class ExternalDbParticipants implements ExternalParticipantSource {
	private int startAt, endAt = 0;

	private DbCfg dbCfg;

	private String dbCfgPath;

	private JdbcTemplate jdbcTemplate;

	private SingleConnectionDataSource dataSource;

	@Autowired
	private DaoFactory daoFactory;

	public DaoFactory getDaoFactory() {
		return daoFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public SingleConnectionDataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(SingleConnectionDataSource dataSource) {
		this.dataSource = dataSource;
	}

	public String getDbCfgPath() {
		return dbCfgPath;
	}

	public void setDbCfgPath(String dbCfgPath) throws JsonParseException, JsonMappingException, IOException {
		this.dbCfgPath = dbCfgPath;
		this.dbCfg = parseJson(dbCfgPath);
		this.endAt = dbCfg.getMaxResults();
	}

	@Override
	public String getName() {
		return dbCfg.getName();
	}

	@Override
	public void shutdown() throws SQLException {
		this.dataSource.destroy();
		this.jdbcTemplate = null;
		this.dataSource = null;
		this.startAt = 0;
		this.endAt += dbCfg.getMaxResults();
	}

	@Override
	public List<StagedParticipantDetail> getParticipants() {
		if (jdbcTemplate == null) {
			createConn(dbCfg);
		}

		return jdbcTemplate.query(dbCfg.getSql(), 
			new PreparedStatementSetter() {
		        	public void setValues(PreparedStatement preparedStatement) throws SQLException {
		            		preparedStatement.setInt(2, startAt);
		            		preparedStatement.setInt(1, endAt);
		        	}
		        }, 
			new ResultSetExtractor<List<StagedParticipantDetail>>() {
				@Override
				public List<StagedParticipantDetail> extractData(ResultSet rs) 
				throws SQLException, DataAccessException {
					List<StagedParticipantDetail> participants = new ArrayList<>();

					while (rs.next()) {
						participants.add(toStagedParticipantDetails(rs, dbCfg.name));
					}

					startAt = endAt;
					endAt += dbCfg.getMaxResults();

					return participants;
				}
		});
	}

	@Override
	public Integer getMaxResults() {
		return dbCfg.getMaxResults();
	}

	private void createConn(DbCfg dbCfg) {
		this.dataSource = new SingleConnectionDataSource(dbCfg.getDbUrl(), dbCfg.getDbUser(), dbCfg.getDbPassword(), false);
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	private StagedParticipantDetail toStagedParticipantDetails(ResultSet rs, String source) throws SQLException {
		StagedParticipantDetail input = new StagedParticipantDetail();

		input.setEmpi(rs.getString("EMPI_ID"));
		input.setGender(getGender(rs.getString("GENDER"), source));
		input.setLastName(rs.getString("LAST_NAME"));
		input.setFirstName(rs.getString("FIRST_NAME"));
		input.setMiddleName(rs.getString("MIDDLE_NAME"));
		input.setBirthDate(rs.getDate("BIRTH_DATE"));
		input.setSource(source);

		return input;
	}
	
	private String getGender(String gender, String source) {
		return getMappedValue(PvAttributes.GENDER, gender, source);
	}

	private String getMappedValue(String attribute, String value, String source) {
		if (StringUtils.isBlank(value)) {
			return null;
		}

		List<PermissibleValue> pvs = getMappedPvs(attribute, value, source);
		if (CollectionUtils.isEmpty(pvs)) {
			throw OpenSpecimenException.userError(ExtPartImpErrorCode.VAL_NOT_MAPPED, attribute, value);
		}

		return pvs.iterator().next().getValue();
	}

	@PlusTransactional
	private List<PermissibleValue> getMappedPvs(String attribute, String value, String source) {
		return daoFactory.getPermissibleValueDao().getByPropertyKeyValue(attribute, source, value);
	}

	private DbCfg parseJson(String dbCfgPath) throws JsonParseException, JsonMappingException, IOException {
		return new ObjectMapper().readValue(new File(dbCfgPath), ExternalDbParticipants.DbCfg.class);
	}

	private static class DbCfg {
		String name;

		String dbUrl;

		String dbUser;

		String dbPassword;

		String sql;

		Integer maxResults;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDbUrl() {
			return dbUrl;
		}

		public void setDbUrl(String dbUrl) {
			this.dbUrl = dbUrl;
		}

		public String getSql() {
			return sql;
		}

		public void setSql(String sql) {
			this.sql = sql;
		}

		public Integer getMaxResults() {
			return maxResults == null ? DEF_MAX_RESULTS : maxResults;
		}

		public void setMaxResults(Integer maxResults) {
			this.maxResults = maxResults;
		}

		public String getDbUser() {
			return dbUser;
		}

		public void setDbUser(String dbUser) {
			this.dbUser = dbUser;
		}

		public String getDbPassword() {
			return dbPassword;
		}

		public void setDbPassword(String dbPassword) {
			this.dbPassword = dbPassword;
		}

		private final Integer DEF_MAX_RESULTS = 25;
	}
}
