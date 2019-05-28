package com.krishagni.openspecimen.msk2.repository.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.krishagni.openspecimen.msk2.events.CarsStudyDetail;
import com.krishagni.openspecimen.msk2.events.CollectionDetail;
import com.krishagni.openspecimen.msk2.events.TimepointDetail;
import com.krishagni.openspecimen.msk2.repository.CarsStudyReader;

public class CarsStudyReaderImpl implements CarsStudyReader {
	private SingleConnectionDataSource dataSource;

	private JdbcTemplate jdbcTemplate;

	private List<String> irbNumbers;

	private int currentStudyIdx;

	public CarsStudyReaderImpl(String url, String username, String password) {
		dataSource   = new SingleConnectionDataSource(url, username, password, false);
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public CarsStudyDetail next() {
		if (irbNumbers == null) {
			irbNumbers = getIrbNumbers();
			currentStudyIdx = -1;
		}

		return ++currentStudyIdx < irbNumbers.size() ? getStudyDetail(irbNumbers.get(currentStudyIdx)) : null;
	}

	@Override
	public void close() {
		dataSource.destroy();
	}

	private List<String> getIrbNumbers() {
		return jdbcTemplate.query(
			GET_IRB_NUMBERS_SQL,
			new ResultSetExtractor<List<String>>() {
				@Override
				public List<String> extractData(ResultSet rs)
				throws SQLException, DataAccessException {
					List<String> result = new ArrayList<>();
					while (rs.next()) {
						result.add(rs.getString(1));
					}

					return result;
				}
			}
		);
	}

	private CarsStudyDetail getStudyDetail(String irbNumber) {
		return jdbcTemplate.query(GET_STUDY_DETAILS, new Object[] { irbNumber }, new StudyDetailExtractor());
	}

	private class StudyDetailExtractor implements ResultSetExtractor<CarsStudyDetail> {
		@Override
		public CarsStudyDetail extractData(ResultSet rs)
		throws SQLException, DataAccessException {
			CarsStudyDetail study = null;
			Map<String, TimepointDetail> timepointsMap = new LinkedHashMap<>();

			while (rs.next()) {
				if (study == null) {
					study = new CarsStudyDetail();
					study.setIrbNumber(rs.getString("irbnumber"));
					study.setFacility(rs.getString("facility"));
					study.setPiAddress(rs.getString("email").trim());
					study.setPiFirst("FirstName");
					study.setPiLast("LastName");
				}

				String timepointKey = rs.getString("cyclename") + " " + rs.getString("timepointname");
				TimepointDetail timepoint = timepointsMap.get(timepointKey);
				if (timepoint == null) {
					timepoint = getTimepoint(rs);
					timepointsMap.put(timepointKey, timepoint);
				}

				CollectionDetail collection = getCollection(rs);
				timepoint.addCollection(collection);
			}

			if (study != null) {
				study.setTimepoints(new ArrayList<>(timepointsMap.values()));
			}

			return study;
		}

		private TimepointDetail getTimepoint(ResultSet rs)
		throws SQLException {
			TimepointDetail timepoint = new TimepointDetail();
			timepoint.setId(rs.getString("timepointid2"));
			timepoint.setCycle(rs.getString("cyclename"));
			timepoint.setName(rs.getString("timepointname"));
			return timepoint;
		}

		private CollectionDetail getCollection(ResultSet rs)
		throws SQLException {
			CollectionDetail collection = new CollectionDetail();
			collection.setId(rs.getString("pvpid2"));
			collection.setName(rs.getString("procedurename"));
			collection.setType(rs.getString("specimentype"));
			collection.setContainer(rs.getString("collectioncontainer"));
			return collection;
		}
	}

	private static final String GET_IRB_NUMBERS_SQL = "select distinct irbnumber from openspecimen.vw_os_coll_prot order by irbnumber";

	private static final String GET_STUDY_DETAILS =
		"select" +
		"  irbnumber, email, cyclename, timepointname, procedurename, facility, " +
		"  collectiontypename, comments, specimentype, collectioncontainer, pvpid2, timepointid2 " +
		"from " +
		"  openspecimen.vw_os_coll_prot " + 
		"where " +
		"  irbnumber = ? ";
}
