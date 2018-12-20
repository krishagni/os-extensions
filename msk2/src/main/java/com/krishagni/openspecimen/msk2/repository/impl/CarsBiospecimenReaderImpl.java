package com.krishagni.openspecimen.msk2.repository.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
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
			query = String.format(GET_PATIENT_IDS_SQL, "where kitprepdate > ?");
			params.add(updatedAfter);
		} else {
			query = String.format(GET_PATIENT_IDS_SQL, "");
		}

		return jdbcTemplate.query(
			query,
			params.toArray(new Date[0]),
			(rs) -> {
				List<Pair<String, String>> patientIds = new ArrayList<>();

				while(rs.next()) {
					patientIds.add(Pair.make(rs.getString("irbnumber"), rs.getString("patientsystemid")));
				}

				return patientIds;
			}
		);
	}
	
	private CarsBiospecimenDetail getParticipant(String irbNumber, String patientId, Date updatedAfter) {
		List<Object> params = new ArrayList<>();
		params.add(irbNumber);
		params.add(patientId);

		String query;
		if (updatedAfter != null) {
			query = String.format(GET_PARTICIPANTS_SQL, "and kitprepdate > ?");
			params.add(updatedAfter);
		} else {
			query = String.format(GET_PARTICIPANTS_SQL, "");
		}

		return jdbcTemplate.query(query, params.toArray(new Object[0]), new ParticipantDetailExtractor());
	}
	
	private class ParticipantDetailExtractor implements ResultSetExtractor<CarsBiospecimenDetail> {
		@Override
		public CarsBiospecimenDetail extractData(ResultSet rs)
		throws SQLException, DataAccessException {
			CarsBiospecimenDetail participant = null;
			Map<String, TimepointDetail> timepoints = new LinkedHashMap<>();
			Map<String, CollectionDetail> collections = new LinkedHashMap<>();

			while (rs.next()) {
				participant = new CarsBiospecimenDetail();
				participant.setPatientId(rs.getString("patientsystemid"));
				participant.setIrbNumber(rs.getString("irbnumber"));
				participant.setPatientStudyId(rs.getString("patientstudyid"));
				participant.setFacility(rs.getString("facility"));
				participant.setFirstName(rs.getString("firstname"));
				participant.setLastName(rs.getString("lastname"));
				participant.setMiddleName(rs.getString("middlename"));
				participant.setDob(rs.getDate("dob"));
				participant.setMrn(rs.getString("patientmrn"));

				String timepointId = rs.getString("timepointid");
				TimepointDetail timepoint = timepoints.computeIfAbsent(timepointId, (k) -> new TimepointDetail());
				timepoint.setId(rs.getString("timepointid"));
				timepoint.setName(rs.getString("collectionrequestbasketid"));
				timepoint.setCreationTime(rs.getDate("startdate"));

				String collectionId = rs.getString("pvpid");
				CollectionDetail collection = collections.get(timepointId + "-" + collectionId);
				if (collection == null) {
					collection = new CollectionDetail();
					collection.setId(collectionId);
					timepoint.getCollections().add(collection);
					collections.put(timepointId + "-" + collectionId, collection);
				}

				collection.setName(rs.getString("collectionrequestbasketentriesid"));
				collection.setCreationTime(getCollectionTime(timepoint.getCreationTime(), rs.getString("time")));
				collection.setComments(rs.getString("notes"));
				collection.setProcessed(rs.getInt("processed"));
				collection.setShipped(rs.getInt("shipped"));

				participant.setLastUpdated(rs.getTimestamp("kitprepdate"));
			}

			if (participant != null) {
				participant.setTimepoints(new ArrayList<>(timepoints.values()));
			}

			return participant;
		}
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
		"  distinct irbnumber, patientsystemid " +
		"from " + 
		"  openspecimen.xavier_view_get_requested_collections_v op " +
		" %s	" +
		"order by " + 
		"  kitprepdate";
	
	private final static String GET_PARTICIPANTS_SQL =
		"select " +
		"  patientsystemid, irbnumber, patientstudyid, facility, " +
		"  firstname, lastname, middlename, dob, patientmrn, " +
		"  timepointid, collectionrequestbasketid, startdate, " +
		"  collectionrequestbasketentriesid, time, notes, processed, shipped, " +
		"  kitprepdate " +
		"from " +
		"  openspecimen.xavier_view_get_requested_collections_v " +
		"where " +
		"  irbnumber = ? and " +
		"  patientsystemid = ? " +
		"  %s " +
		"order by " +
		"  kitprepdate";
}
