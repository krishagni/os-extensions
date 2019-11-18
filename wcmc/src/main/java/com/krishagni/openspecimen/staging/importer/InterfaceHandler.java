package com.krishagni.openspecimen.staging.importer;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.events.StagedParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.PvAttributes;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.openspecimen.staging.importer.error.StgImpErrorCode;
import com.krishagni.openspecimen.staging.importer.props.StgImpPropConfig;

@Configurable
public abstract class InterfaceHandler {
	private static final Log logger = LogFactory.getLog(InterfaceHandler.class);

	private SourceDbManager sourceDbManager;

	@Autowired
	private DaoFactory daoFactory;

	public DaoFactory getDaoFactory() {
		return daoFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public abstract StagedParticipantDetail saveStagedParticipant(StagedParticipantDetail stagedParticipant);

	public abstract List<Object> getSqlArgs();

	public void setupHandler() {
		try {
			sourceDbManager = new SourceDbManager();

			sourceDbManager.connect(
					StgImpPropConfig.getInstance().getDbUrl(),
					StgImpPropConfig.getInstance().getDbUser(),
					StgImpPropConfig.getInstance().getDbPwd());
		
			sourceDbManager.loadJson(StgImpPropConfig.getInstance().getJsonMapping());
		} catch (IOException e) {
			logger.error("Error loading the JSON file", e);
		}
	}

	public void formatSql(String... args) {
		sourceDbManager.formatSql(args);
	}

	public void processStagedParticipants() {
		sourceDbManager.getJdbcTemplate().query(sourceDbManager.getSql(), 
				getSqlArgs().toArray(),
				new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				// 1. Transform it to StagedParticipantDetails
				StagedParticipantDetail stagedParticipant = toStagedParticipantDetails(rs);
				// 2. Pass it down to savedStagedParticipant.
				saveStagedParticipant(stagedParticipant);
			}
		});

		sourceDbManager.closeConnection();
	}

	private StagedParticipantDetail toStagedParticipantDetails(ResultSet rs) throws SQLException {
		StagedParticipantDetail input = new StagedParticipantDetail();

		input.setEmpi(getStringIfColExists(rs, "EMPI_ID"));
		input.setUid(getStringIfColExists(rs, "SSN"));
		input.setEthnicities(getEthnicities(getStringIfColExists(rs, "ETHNICITY")));
		input.setGender(getGender(getStringIfColExists(rs, "GENDER")));
		input.setLastName(getStringIfColExists(rs, "LAST_NAME"));
		input.setFirstName(getStringIfColExists(rs, "FIRST_NAME"));
		input.setMiddleName(getStringIfColExists(rs, "MIDDLE_NAME"));
		input.setBirthDate(getDateIfColExists(rs, "BIRTH_DATE"));
		input.setRaces(getRaces(getStringIfColExists(rs, "RACE")));
		input.setSource(sourceDbManager.getSource());
		input.setVitalStatus(getStringIfColExists(rs, "VITAL_STATUS"));
		input.setDeathDate(getDateIfColExists(rs, "DEATH_DATE"));

		return input;
	}

	private String getStringIfColExists(ResultSet rs, String columnName) throws SQLException {
		if (hasColumn(rs, columnName) ) {
			return rs.getString(columnName);
		}

		logger.info(String.format("Column %s in does not exists in the source database.", columnName));
		return null;
	}

	private Date getDateIfColExists(ResultSet rs, String columnName) throws SQLException {
		if (hasColumn(rs, columnName) ) {
			return rs.getDate(columnName);
		}

		logger.info(String.format("Column %s in does not exists in the source database.", columnName));
		return null;
	}

	private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
	    ResultSetMetaData rsmd = rs.getMetaData();
	    int columns = rsmd.getColumnCount();

	    for (int i = 1; i <= columns; i++) {
	        if (columnName.equalsIgnoreCase(rsmd.getColumnLabel(i))) {
	            return true;
	        }
	    }

	    return false;
	}

	private Set<String> getEthnicities(String ethnicity0) {
		String ethnicity = getMappedValue(PvAttributes.ETHNICITY, ethnicity0);

		if (StringUtils.isNotBlank(ethnicity)) {
			return Collections.singleton(ethnicity);
		} else {
			return Collections.emptySet();
		}
	}

	private String getGender(String gender) {
		return getMappedValue(PvAttributes.GENDER, gender);
	}

	private Set<String> getRaces(String race) {
		Set<String> races = new HashSet<>();

		if (StringUtils.isNotBlank(race)) {
			races.add(getMappedValue(PvAttributes.RACE, race));
		}

		return races;
	}

	private String getMappedValue(String attribute, String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}

		List<PermissibleValue> pvs = getMappedPvs(attribute, value);
		if (CollectionUtils.isEmpty(pvs)) {
			throw OpenSpecimenException.userError(StgImpErrorCode.VAL_NOT_MAPPED, attribute, value);
		}

		return pvs.iterator().next().getValue();
	}

	@PlusTransactional
	private List<PermissibleValue> getMappedPvs(String attribute, String value) {
		return daoFactory.getPermissibleValueDao().getByPropertyKeyValue(attribute, sourceDbManager.getSource(), value);
	}
}
