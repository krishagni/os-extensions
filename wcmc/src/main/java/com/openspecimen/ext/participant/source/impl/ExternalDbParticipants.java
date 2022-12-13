package com.openspecimen.ext.participant.source.impl;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
import com.openspecimen.ext.participant.crit.ExtParticipantListCriteria;
import com.openspecimen.ext.participant.error.ExtPartImpErrorCode;
import com.openspecimen.ext.participant.source.ExternalParticipantSource;

public class ExternalDbParticipants implements ExternalParticipantSource {
	private static final Log logger = LogFactory.getLog(ExternalDbParticipants.class);

	private DbCfg dbCfg;

	private String dbCfgPath;

	private NamedParameterJdbcTemplate jdbcTemplate;

	private SingleConnectionDataSource dataSource;

	private String source;

	private DaoFactory daoFactory;

	public SingleConnectionDataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(SingleConnectionDataSource dataSource) {
		this.dataSource = dataSource;
	}

	public String getDbCfgPath() {
		return dbCfgPath;
	}

	public void setDbCfgPath(String dbCfgPath) {
		this.dbCfgPath = dbCfgPath;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public DaoFactory getDaoFactory() {
		return daoFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public void init() {
		try {
			this.dbCfg = parseJson(dbCfgPath);
		} catch (Exception e) {
			logger.error("Error occured while initializing external participant source", e);
		}
	}

	@Override
	public String getName() {
		return source;
	}

	@Override
	public void cleanUp() {
		this.dataSource.destroy();
		this.jdbcTemplate = null;
		this.dataSource = null;
	}

	@Override
	public List<StagedParticipantDetail> getParticipants(ExtParticipantListCriteria criteria) {
		if (jdbcTemplate == null) {
			createConn(dbCfg);
		}

		return jdbcTemplate.query(dbCfg.getSql(), 
			getParams(criteria), 
			new ResultSetExtractor<List<StagedParticipantDetail>>() {
				@Override
				public List<StagedParticipantDetail> extractData(ResultSet rs) 
				throws SQLException, DataAccessException {
					List<StagedParticipantDetail> participants = new ArrayList<>();

					while (rs.next()) {
						participants.add(toStagedParticipantDetails(rs));
					}

					criteria.startAt(participants.size() + criteria.startAt());

					return participants;
				}
		});
	}

	private MapSqlParameterSource getParams(ExtParticipantListCriteria criteria) {
		if (criteria == null) {
			return new MapSqlParameterSource(Collections.emptyMap());
		}

		Map<String, Object> params = new HashMap<>();

		params.put("startAt", criteria.startAt());
		params.put("endAt", criteria.startAt() + criteria.maxResults());
		params.put("lastRun", criteria.lastRun() != null ? criteria.lastRun() : new Date(0));

		return new MapSqlParameterSource(params);
	}

	private void createConn(DbCfg dbCfg) {
		this.dataSource = new SingleConnectionDataSource(dbCfg.getDbUrl(), dbCfg.getDbUser(), dbCfg.getDbPassword(), false);
		this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
	}

	private StagedParticipantDetail toStagedParticipantDetails(ResultSet rs) throws SQLException {
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
		String dbUrl;

		String dbUser;

		String dbPassword;

		String sql;

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
	}
}
