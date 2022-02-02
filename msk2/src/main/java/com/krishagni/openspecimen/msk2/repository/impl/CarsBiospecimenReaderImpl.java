package com.krishagni.openspecimen.msk2.repository.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.openspecimen.msk2.events.CarsBiospecimenDetail;
import com.krishagni.openspecimen.msk2.events.CollectionDetail;
import com.krishagni.openspecimen.msk2.events.TimepointDetail;
import com.krishagni.openspecimen.msk2.repository.CarsBiospecimenReader;

public class CarsBiospecimenReaderImpl implements CarsBiospecimenReader {
	private SingleConnectionDataSource dataSource;

	private JdbcTemplate jdbcTemplate;
	
	private List<Pair<String, String>> patientIds;

	private Date updatedAfter;
	
	private int currentParticipantIdx;
	
	public CarsBiospecimenReaderImpl(String url, String username, String password, Date updatedAfter) {
		dataSource   = new SingleConnectionDataSource(url, username, password, false);
		jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.setResultsMapCaseInsensitive(true);
		this.updatedAfter = updatedAfter;
	}
	
	@Override
	public void close() {
		dataSource.destroy();
	}

	@Override
	public CarsBiospecimenDetail next() {
		if (patientIds == null) {
			patientIds = getPatientIds();
			currentParticipantIdx = -1;
		}

		++currentParticipantIdx;
		if (currentParticipantIdx >= patientIds.size()) {
			return null;
		}

		Pair<String, String> patientId = patientIds.get(currentParticipantIdx);
		return getParticipant(patientId.first(), patientId.second(), updatedAfter);
	}
	
	private List<Pair<String, String>> getPatientIds() {
		List<Object> params = new ArrayList<>();

		String query;
		if (updatedAfter != null) {
			query = String.format(GET_PATIENT_IDS_SQL, "where updateddate > ? or kitprepdate > ?");
			params.add(updatedAfter);
			params.add(updatedAfter);
		} else {
			query = String.format(GET_PATIENT_IDS_SQL, "");
		}

		return jdbcTemplate.query(
			query,
			params.toArray(new Date[0]),
			new ResultSetExtractor<List<Pair<String, String>>>() {
				@Override
				public List<Pair<String, String>> extractData(ResultSet rs)
				throws SQLException, DataAccessException {
					Set<Pair<String, String>> patientIds = new LinkedHashSet<>();
					while (rs.next()) {
						patientIds.add(Pair.make(rs.getString("irbnumber"), rs.getString("patientsystemid")));
					}

					return new ArrayList<>(patientIds);
				}
			}
		);
	}
	
	private CarsBiospecimenDetail getParticipant(String irbNumber, String patientId, Date updatedAfter) {
		Map<String,Object> params = new HashMap<>();
		params.put("IRBNUM", irbNumber);
		params.put("PATIENT_SYSTEMID", patientId);
		params.put("JOBLASTRUN", (updatedAfter != null ? updatedAfter : getDefaultDate()));

		SimpleJdbcCall proc = new SimpleJdbcCall(jdbcTemplate)
			.withProcedureName("openspecimen.xavier_proc_get_requested_collections_v")
			.withoutProcedureColumnMetaDataAccess()
			.returningResultSet("rows", new ParticipantDetailMapper());

		proc.addDeclaredParameter(new SqlParameter("IRBNUM", Types.VARCHAR));
		proc.addDeclaredParameter(new SqlParameter("PATIENT_SYSTEMID", Types.VARCHAR));
		proc.addDeclaredParameter(new SqlParameter("JOBLASTRUN", Types.TIMESTAMP));

		SqlParameterSource in = new MapSqlParameterSource(params);
		List<CarsBiospecimenDetail> detailObject = (List) proc.execute(in).get("rows");
		return detailObject.get(0);
	}
	
	private class ParticipantDetailMapper implements RowMapper<CarsBiospecimenDetail> {

		@Override
		public CarsBiospecimenDetail mapRow(ResultSet rs, int rowNum) 
		throws SQLException {
			CarsBiospecimenDetail participant = null;
			Map<String, TimepointDetail> timepoints = new LinkedHashMap<>();
			Map<String, CollectionDetail> collections = new LinkedHashMap<>();

			boolean hasRow = true;

			while (hasRow) {
				participant = new CarsBiospecimenDetail();
				participant.setPatientId(rs.getString("patientsystemid"));
				participant.setIrbNumber(rs.getString("irbnumber"));
				String patientStudyId = rs.getString("patientstudyid");
				participant.setPatientStudyId("NOT_ENTERED".equalsIgnoreCase(patientStudyId) ? null : patientStudyId);
				participant.setFacility(getString(rs, "facility"));
				participant.setFirstName(rs.getString("firstname"));
				participant.setLastName(rs.getString("lastname"));
				participant.setMiddleName(rs.getString("middlename"));
				participant.setDob(rs.getDate("dob"));
				participant.setMrn(rs.getString("patientmrn"));

				String timepointId = rs.getString("timepointid2");
				TimepointDetail timepoint = timepoints.computeIfAbsent(timepointId, (k) -> new TimepointDetail());
				timepoint.setId(timepointId);
				timepoint.setName(rs.getString("collectionrequestbasketid"));
				timepoint.setCreationTime(rs.getDate("startdate"));

				String collectionId = rs.getString("pvpid2");
				String spmnName = rs.getString("collectionrequestbasketentriesid");
				CollectionDetail collection = collections.get(rs.getString("irbnumber") + "-" + spmnName);
				if (collection == null) {
					collection = new CollectionDetail();
					collection.setId(collectionId);
					timepoint.getCollections().add(collection);
					collections.put(rs.getString("irbnumber") + "-" + spmnName, collection);
				}

				collection.setName(spmnName);
				collection.setCreationTime(getCollectionTime(timepoint.getCreationTime(), rs.getString("time")));
				collection.setComments(rs.getString("notes"));
				collection.setProcessed(rs.getInt("processed"));
				collection.setShipped(rs.getInt("shipped"));

				participant.setLastUpdated(rs.getTimestamp("updateddate"));

				hasRow = rs.next();
			}

			if (participant != null) {
				participant.setTimepoints(new ArrayList<>(timepoints.values()));
			}

			return participant;
		}
	}
	
	private Date getDefaultDate() {
		return new GregorianCalendar(1900, 0, 1).getTime();
	}

	private String getString(ResultSet rs, String columnName) throws SQLException {
		String result = rs.getString(columnName);
		return result != null ? result.trim() : null;
	}

	private Date getCollectionTime(Date visitDate, String time) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(visitDate);

		if (time != null) {
			String[] parts = time.split(":");

			if (parts.length > 0) {
				cal.set(Calendar.HOUR, parseInt(parts[0]));
			}

			if (parts.length > 1) {
				cal.set(Calendar.MINUTE, parseInt(parts[1]));
			}
		}

		return cal.getTime();
	}

	private int parseInt(String input) {
		try {
			return Integer.parseInt(input);
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}

	private final static String GET_PATIENT_IDS_SQL =
		"select " + 
		"  irbnumber, patientsystemid " +
		"from " + 
		"  openspecimen.xavier_refresh_get_requested_collections_v_t ip " +
		"  %s	" +
		"order by " + 
		"  updateddate";
}