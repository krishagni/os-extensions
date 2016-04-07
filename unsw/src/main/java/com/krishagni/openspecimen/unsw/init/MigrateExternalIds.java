package com.krishagni.openspecimen.unsw.init;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.util.CsvFileReader;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail.AttrDetail;

public class MigrateExternalIds implements InitializingBean {
	private static Log logger = LogFactory.getLog(MigrateExternalIds.class);
	
	private PlatformTransactionManager txnMgr;
	
	private DaoFactory daoFactory;
	
	private JdbcTemplate jdbcTemplate;
	
	private Map<String, String> mappingExternalIds = new HashMap<String, String>();

	public void setTxnMgr(PlatformTransactionManager txnMgr) {
		this.txnMgr = txnMgr;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("Migration Of Specimen's External Ids started...");
		populateExternalIdsMap();
		migrateExternalIdData();
	}
	
	public void migrateExternalIdData() {
		Date startTime = Calendar.getInstance().getTime();
		logger.info("Migration from static table start time: " + startTime);

		TransactionTemplate txnTmpl = new TransactionTemplate(txnMgr);
		txnTmpl.execute(new TransactionCallback<Integer>() {
			@Override
			public Integer doInTransaction(TransactionStatus status) {
				setUserInContextHolder();
				
				List<Long> specimenIds = new ArrayList<Long>();
				Collection<ExternalIdDetail> externalIdDetails = getExternalIdsDetail(GET_EXTERNAL_IDS_SQL);
				for (ExternalIdDetail detail : externalIdDetails) {
					Specimen specimen = daoFactory.getSpecimenDao().getById(detail.getSpecimenId());
					if (specimen == null) {
						continue;
					}
					
					ExtensionDetail extnDetail = new ExtensionDetail();

					AttrDetail attr = new AttrDetail();
					attr.setName("externalIDs");
					attr.setValue(detail.getExternalIds());
					extnDetail.setAttrs(Collections.singletonList(attr));
					
					specimen.setExtension(DeObject.createExtension(extnDetail, specimen));
					specimen.addOrUpdateExtension();
					
					specimenIds.add(detail.getSpecimenId());
				}
				
				if (CollectionUtils.isNotEmpty(specimenIds)) {
					deleteMigratedData(specimenIds);
				}
				
				logger.info("Migration completed successfully...");
				System.out.println("Migration completed successfully...");
				return 1;
			}
		});
	}
	
	private void setUserInContextHolder() {
		String adminEmailId = "admin@unsw.edu.au"; //"admin@admin.com";
		String adminDomain = "openspecimen";
		User user = daoFactory.getUserDao().getUser(adminEmailId, adminDomain);
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user,null, user.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(token);
	}
	
	private Collection<ExternalIdDetail> getExternalIdsDetail(String getExternalIdDetail) {
		Map<Long, ExternalIdDetail> externalIdDetails = new HashMap<Long, ExternalIdDetail>();
		
		jdbcTemplate.query(getExternalIdDetail, new RowCallbackHandler() {

			@Override
			public void processRow(ResultSet rs) throws SQLException {
				Long specimenId = rs.getLong("specimen_id");
				String externalId = rs.getString("name").trim();
				String value = rs.getString("value");
				
				ExternalIdDetail detail = externalIdDetails.get(specimenId);
				if (detail == null) {
					detail = new ExternalIdDetail(specimenId);
					externalIdDetails.put(specimenId, detail);
				}
					
				detail.addExternalId(externalId.trim(), value);
			}
		});
		
		return externalIdDetails.values();
	}
	
	private void deleteMigratedData(List<Long> ids) {
		int startIdx = 0;
		int count = 1;
		
		while (startIdx < ids.size()) {
			int lastIdx = count * 999 < ids.size() ? count * 999 : ids.size();
			jdbcTemplate.update(String.format(DELETE_MIGRATED_DATA, StringUtils.join(ids.subList(startIdx, lastIdx), ",")));
			startIdx = lastIdx;
			count++;
		}
		
		jdbcTemplate.update(DELETE_NULL_RECORDS);
		logger.info("Deleted record for specimen id : " + StringUtils.join(ids, ","));
	}
	
	private void populateExternalIdsMap() throws IOException {
		InputStream in = getClass().getResourceAsStream("/Mapping_ExternalIDs.csv"); 
		CsvFileReader reader = CsvFileReader.createCsvFileReader(in, true);
		
		while (reader.next()) {
			String row[] = reader.getRow();
			mappingExternalIds.put(row[0].trim(), row[1].trim());
		}
	}

	private static final String GET_EXTERNAL_IDS_SQL = 
			"select * from catissue_external_identifier where name is not null and value is not null"; 
	
	private static final String DELETE_MIGRATED_DATA = "delete from catissue_external_identifier where specimen_id in (%s)";
	
	private static final String DELETE_NULL_RECORDS = "delete from catissue_external_identifier where name is null and value is null";
	
	private class ExternalIdDetail {
		private Long specimenId;
		
		private List<Map<String, String>> externalIds = new ArrayList<Map<String, String>>();
		
		public ExternalIdDetail(Long specimenId) {
			this.specimenId = specimenId;
		}

		public Long getSpecimenId() {
			return specimenId;
		}
		
		public List<Map<String, String>> getExternalIds() {
			return externalIds;
		}

		public void addExternalId(String externalId, String externalValue) {
			externalId = mappingExternalIds.containsKey(externalId) ? mappingExternalIds.get(externalId) : externalId;
			
			Map<String, String> value = new HashMap<String, String>();
			value.put("externalID", externalId);
			value.put("externalValue", externalValue);
			
			externalIds.add(value);
		}
	}
}