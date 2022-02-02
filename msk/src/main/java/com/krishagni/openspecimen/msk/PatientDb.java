package com.krishagni.openspecimen.msk;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class PatientDb implements Closeable {

	private SingleConnectionDataSource ds;

	private JdbcTemplate jdbcTemplate;

	public PatientDb() {
		try {
			Class.forName("oracle.jdbc.OracleDriver");
		} catch (Exception e) {
			e.printStackTrace();
		}


		ds = new SingleConnectionDataSource(
			ConfigParams.getUrl(),
			ConfigParams.getUsername(),
			ConfigParams.getPassword(),
			false);

		jdbcTemplate = new JdbcTemplate(ds);
	}

	@Override
	public void close() {
		ds.destroy();
	}

	public Patient getByMrn(String mrn) {
		return jdbcTemplate.query(GET_BY_MRN_SQL, new String[] { mrn }, new ResultSetExtractor<Patient>() {
			@Override
			public Patient extractData(ResultSet rs)
			throws SQLException, DataAccessException {
				try {
					Patient patient = null;

					while (rs.next()) {
						if (patient == null) {
							patient = new Patient();
							patient.setMrn(getMrn(rs));
							patient.setAlive(isAlive(rs));
						}

						patient.addProtocolConsent(getProtocol(rs), getRegId(rs), getConsented(rs), getQuestion(rs), getAnswer(rs));
					}

					return patient;
				} finally {
					rs.close();
				}
			}

			private String getMrn(ResultSet rs)
			throws SQLException {
				return rs.getString(1);
			}

			private String getProtocol(ResultSet rs)
			throws SQLException {
				return rs.getString(2);
			}

			private String getRegId(ResultSet rs)
			throws SQLException {
				return rs.getString(3);
			}

			private Boolean getConsented(ResultSet rs)
			throws SQLException {
				String consented = rs.getString(4);
				return StringUtils.equalsIgnoreCase(consented, YES);
			}

			private boolean isAlive(ResultSet rs)
			throws SQLException {
				Date survivalDt = rs.getDate(5);
				String alive = rs.getString(6);
				if (alive == null && survivalDt == null) {
					return true;
				}

				return !StringUtils.equalsIgnoreCase(alive, DEAD) && (survivalDt == null);
			}

			private String getQuestion(ResultSet rs)
			throws SQLException {
				return rs.getString(7);
			}

			private String getAnswer(ResultSet rs)
			throws SQLException {
				return rs.getString(8);
			}
		});
	}

	private static final String YES = "Yes";

	private static final String DEAD = "Dead";

	private static final String GET_BY_MRN_SQL =
		"select " +
		"  p.pt_mrn, reg.prt_id, reg.reg_id, cs.crnt_consent_status_dscrp, " +
		"  p.survival_date, ps.survival_status_dscrp, q.question_dscrp, a.answer_dscrp " +
		"from " +
		"  openspecimen_pt p " +
		"  left join openspecimen_survival_status ps on ps.survival_status = p.survival_status " +
		"  left join openspecimen_pt_reg reg on reg.pt_mrn = p.pt_mrn " +
		"  left join openspecimen_crnt_consent_sts cs on cs.crnt_consent_status = reg.crnt_consent_status " +
		"  left join openspecimen_consent_q cq on cq.pt_mrn = p.pt_mrn " +
		"  left join openspecimen_consent_question q on q.question_id = cq.question_id " +
		"  left join openspecimen_consent_answer a on a.answer = cq.answer " +
		"where " +
		"  p.pt_mrn = ? " +
		"order by " +
		"  reg.prt_id asc, reg.consent_date asc, cq.consent_date asc";
}
