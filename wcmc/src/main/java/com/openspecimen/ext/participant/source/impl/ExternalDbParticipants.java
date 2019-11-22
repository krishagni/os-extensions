package com.openspecimen.ext.participant.source.impl;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
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
	private String dbCfgPath;

	private DbCfg dbCfg;

	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DaoFactory daoFactory;

	public DaoFactory getDaoFactory() {
		return daoFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setDbCfgPath(String dbCfgPath) throws JsonParseException, JsonMappingException, IOException {
		this.dbCfgPath = dbCfgPath;
		this.dbCfg = parseJson(dbCfgPath);
	}

	@Override
	public String getName() {
		return dbCfg.getName();
	}

	@Override
	public List<StagedParticipantDetail> getParticipants() {
		if (jdbcTemplate == null) {
			createConn(dbCfg);
		}

		return jdbcTemplate.query(dbCfg.getSql(), 
			new ResultSetExtractor<List<StagedParticipantDetail>>() {
				@Override
				public List<StagedParticipantDetail> extractData(ResultSet rs)
						throws SQLException, DataAccessException {
					List<StagedParticipantDetail> participants = new ArrayList<>();

					while (rs.next()) {
						participants.add(toStagedParticipantDetails(rs, dbCfg.name));
					}

					return participants;
				}
		});
	}

	private void createConn(DbCfg dbCfg) {
		DataSource source = new SingleConnectionDataSource(dbCfg.getDbUrl(), false);
		this.jdbcTemplate = new JdbcTemplate(source);
	}

	private StagedParticipantDetail toStagedParticipantDetails(ResultSet rs, String source) throws SQLException {
		StagedParticipantDetail input = new StagedParticipantDetail();

		input.setEmpi(rs.getString("EMPI_ID"));
		input.setUid(rs.getString("SSN"));
		input.setEthnicities(getEthnicities(rs.getString("ETHNICITY"), source));
		input.setGender(getGender(rs.getString("GENDER"), source));
		input.setLastName(rs.getString("LAST_NAME"));
		input.setFirstName(rs.getString("FIRST_NAME"));
		input.setMiddleName(rs.getString("MIDDLE_NAME"));
		input.setBirthDate(rs.getDate("BIRTH_DATE"));
		input.setRaces(getRaces(rs.getString("RACE"), source));
		input.setSource(source);
		input.setVitalStatus(rs.getString("VITAL_STATUS"));
		input.setDeathDate(rs.getDate("DEATH_DATE"));

		return input;
	}
	
	private Set<String> getEthnicities(String ethnicity0, String source) {
		String ethnicity = getMappedValue(PvAttributes.ETHNICITY, ethnicity0, source);

		if (StringUtils.isNotBlank(ethnicity)) {
			return Collections.singleton(ethnicity);
		} else {
			return Collections.emptySet();
		}
	}

	private String getGender(String gender, String source) {
		return getMappedValue(PvAttributes.GENDER, gender, source);
	}

	private Set<String> getRaces(String race, String source) {
		Set<String> races = new HashSet<>();

		if (StringUtils.isNotBlank(race)) {
			races.add(getMappedValue(PvAttributes.RACE, race, source));
		}

		return races;
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
		return new ObjectMapper().readValue(new File(dbCfgPath), DbCfg.class);
	}

	private class DbCfg {
		String name;

		String dbUrl;

		String sql;

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
	}
}
