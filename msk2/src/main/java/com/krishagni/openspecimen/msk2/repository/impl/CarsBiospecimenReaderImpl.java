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
			query = String.format(GET_PPIDS_SQL, "and kitprepdate > ?");
			params.add(updatedAfter);
		} else {
			query = String.format(GET_PPIDS_SQL, "");
		}

		return jdbcTemplate.query(
			query,
			params.toArray(new Date[0]),
			new ResultSetExtractor<List<Pair<String, String>>>() {
				@Override
				public List<Pair<String, String>> extractData(ResultSet rs)
				throws SQLException, DataAccessException {
					List<Pair<String, String>> ppids = new ArrayList<>();

					while (rs.next()) {
						ppids.add(Pair.make(rs.getString("irbnumber"), rs.getString("patientstudyid")));
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
			List<CarsBiospecimenDetail> participants = new ArrayList<>();
			
			while (rs.next()) {
				CarsBiospecimenDetail participant = new CarsBiospecimenDetail();

				participant.setIrbNumber(rs.getString("irbnumber"));
				participant.setFacility(rs.getString("facility"));
				participant.setTreatment(rs.getString("patientstudyid"));
				participant.setFirstName(rs.getString("firstname"));
				participant.setLastName(rs.getString("lastname"));
				participant.setMrn(rs.getString("patientmrn"));
				participant.setLastUpdated(rs.getTimestamp("kitprepdate"));
				
				participants.add(participant);
			}
			
			return participants.isEmpty() ? null : participants.get(participants.size() - 1);
		}
	}
	
	private final static String GET_PPIDS_SQL =
		"select " + 
		"  irbnumber, patientstudyid " + 
		"from " + 
		"  openspecimen.xavier_view_get_requested_collections_v op " + 
		"where " + 
		"  kitprepdate in ( " + 
		"    select " + 
		"      min(kitprepdate) " + 
		"    from " + 
		"      openspecimen.xavier_view_get_requested_collections_v ip " + 
		"    where " + 
		"      ip.irbnumber = op.irbnumber and " + 
		"      ip.patientstudyid = op.patientstudyid " + 
		"  ) " +
		" %s	" + 
		"order by " + 
		"  kitprepdate";
	
	private final static String GET_PARTICIPANTS_SQL =
		"select " +
		"  irbnumber, facility, patientstudyid, firstname, " +
		"  lastname, patientmrn, kitprepdate " +
		"from " +
		"  openspecimen.xavier_view_get_requested_collections_v " +
		"where " +
		"  irbnumber = ? and " +
		"  patientstudyid = ? " +
		"  %s " +
		"order by " +
		"  kitprepdate";
}
