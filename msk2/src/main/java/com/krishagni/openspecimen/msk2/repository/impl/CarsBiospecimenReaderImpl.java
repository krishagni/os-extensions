package com.krishagni.openspecimen.msk2.repository.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.openspecimen.msk2.events.CarsBiospecimenDetail;
import com.krishagni.openspecimen.msk2.repository.CarsBiospecimenReader;

public class CarsBiospecimenReaderImpl implements CarsBiospecimenReader {
	private SingleConnectionDataSource dataSource;

	private JdbcTemplate jdbcTemplate;
	
	private List<Pair<String, String>> ppids;

	private List<CarsBiospecimenDetail> currentParticipants;

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
		if (ppids == null) {
			ppids = getPpids();
			currentParticipantIdx = -1;
		}

		++currentParticipantIdx;
		if (currentParticipantIdx >= ppids.size()) {
			return null;
		}

		Pair<String, String> ppid = ppids.get(currentParticipantIdx);
		return getParticipant(ppid.first(), ppid.second(), updatedAfter);
	}
	
	private List<Pair<String, String>> getPpids() {
		List<Object> params = new ArrayList<>();

		String query;
		if (updatedAfter != null) {
			query = String.format(GET_PPIDS_SQL, "where status_updated_on > ?");
			params.add(updatedAfter);
		} else {
			query = String.format(GET_PPIDS_SQL, "");
		}

		return jdbcTemplate.query(
			query,
			params.toArray(new String[0]),
			new ResultSetExtractor<List<Pair<String, String>>>() {
				@Override
				public List<Pair<String, String>> extractData(ResultSet rs)
				throws SQLException, DataAccessException {
					List<Pair<String, String>> ppids = new ArrayList<>();

					while (rs.next()) {
						ppids.add(Pair.make(rs.getString("irbnumber"), rs.getString("treatment")));
					}

					return ppids;
				}
			});
	}
	
	private CarsBiospecimenDetail getParticipant(String irbNumber, String treatment, Date updatedAfter) {
		List<Object> params = new ArrayList<>();
		params.add(irbNumber);
		params.add(treatment);

		String query;
		if (updatedAfter != null) {
			query = String.format(GET_PARTICIPANTS_SQL, "and status_updated_on > ?");
			params.add(updatedAfter);
		} else {
			query = String.format(GET_PARTICIPANTS_SQL, "");
		}

		return jdbcTemplate.query(GET_PARTICIPANTS_SQL, params.toArray(new String[0]), new ParticipantDetailExtractor());
	}
	
	private class ParticipantDetailExtractor implements ResultSetExtractor<CarsBiospecimenDetail> {
		@Override
		public CarsBiospecimenDetail extractData(ResultSet rs)
		throws SQLException, DataAccessException {
			List<CarsBiospecimenDetail> participants = new ArrayList<>();
			
			while (rs.next()) {
				CarsBiospecimenDetail participant = new CarsBiospecimenDetail();

				participant.setIrbNumber(rs.getString("irbnumber"));
				participant.setFacility(rs.getString("facility"));
				participant.setTreatment(rs.getString("treatment"));
				participant.setFirstName(rs.getString("first_name"));
				participant.setLastName(rs.getString("last_name"));
				participant.setMrn(rs.getString("mrn"));
				participant.setLastUpdated(rs.getTimestamp("status_updated_on"));
				
				participants.add(participant);
			}
			
			return participants.isEmpty() ? null : participants.get(participants.size() - 1);
		}
	}
	
	private final static String GET_PPIDS_SQL =
		"select distinct irbnumber, treatment from msk2_participants_view %s order by status_updated_on";
	
	private final static String GET_PARTICIPANTS_SQL =
		"select " +
		"  irbnumber, facility, treatment, first_name, " +
		"  last_name, mrn, status_updated_on " +
		"from " +
		"  msk2_participants_view " +
		"where " +
		"  irbnumber = ? and " +
		"  treatment = ? " +
		"  %s " +
		"order by " +
		"  status_updated_on";
}
