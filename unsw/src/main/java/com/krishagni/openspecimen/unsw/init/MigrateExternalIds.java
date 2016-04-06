package com.krishagni.openspecimen.unsw.init;

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
				Collection<ExternalIdDetail> externalIdDetails = getExternalIdsDetail(GET_EXTERNAL_ID_DETAIL);
				for (ExternalIdDetail detail : externalIdDetails) {
					ExtensionDetail extnDetail = new ExtensionDetail();
					
					AttrDetail attr = new AttrDetail();
					attr.setName("externalIDs");
					attr.setValue(detail.getExternalIds());
					extnDetail.setAttrs(Collections.singletonList(attr));
					
					Specimen specimen = daoFactory.getSpecimenDao().getById(detail.getSpecimenId());
					specimen.setExtension(DeObject.createExtension(extnDetail, specimen));
					specimen.addOrUpdateExtension();
					
					specimenIds.add(detail.getSpecimenId());
				}
				
				deleteMigratedData(specimenIds);
				
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
				String externalId = rs.getString("name");
				String value = rs.getString("value");
				
				ExternalIdDetail detail = externalIdDetails.get(specimenId);
				if (detail == null) {
					detail = new ExternalIdDetail(specimenId);
					externalIdDetails.put(specimenId, detail);
				}
				
				detail.addExternalId(externalId, value);
			}
		});
		
		return externalIdDetails.values();
	}
	
	private void deleteMigratedData(List<Long> ids) {
		jdbcTemplate.update(String.format(DELETE_MIGRATED_DATA, StringUtils.join(ids, ",")));
		logger.info("Deleted record for specimen id : " + StringUtils.join(ids, ","));
	}
	
	private void populateExternalIdsMap() {
		mappingExternalIds.put("ap seals block label", "AP SEALS label");
		mappingExternalIds.put("ap block label", "AP SEALS label");
		mappingExternalIds.put("ap seal block label", "AP SEALS label");
		mappingExternalIds.put("ap seals", "AP SEALS label");
		mappingExternalIds.put("ap seals label", "AP SEALS label");
		mappingExternalIds.put("ap slide label", "AP SEALS label");
		mappingExternalIds.put("block label", "AP SEALS label");
		mappingExternalIds.put("block reserved @ ap seals", "AP SEALS label");
		mappingExternalIds.put("seal block label", "AP SEALS label");
		mappingExternalIds.put("seal block no", "AP SEALS label");
		mappingExternalIds.put("seal block no.", "AP SEALS label");
		mappingExternalIds.put("seals", "AP SEALS label");
		mappingExternalIds.put("seals - pow", "AP SEALS label");
		mappingExternalIds.put("seals ap block label", "AP SEALS label");
		mappingExternalIds.put("seals barcode", "AP SEALS label");
		mappingExternalIds.put("seals block", "AP SEALS label");
		mappingExternalIds.put("seals block label", "AP SEALS label");
		mappingExternalIds.put("seals block lable", "AP SEALS label");
		mappingExternalIds.put("seals block no.", "AP SEALS label");
		mappingExternalIds.put("seals block reserved", "AP SEALS label");
		mappingExternalIds.put("seals label", "AP SEALS label");
		mappingExternalIds.put("seals no.", "AP SEALS label");
		mappingExternalIds.put("seals slice label", "AP SEALS label");
		mappingExternalIds.put("seals slide labe", "AP SEALS label");
		mappingExternalIds.put("seals slide label", "AP SEALS label");
		mappingExternalIds.put("seals slide lable", "AP SEALS label");
		mappingExternalIds.put("seals slide lablel", "AP SEALS label");
		mappingExternalIds.put("seals slide no.", "AP SEALS label");
		mappingExternalIds.put("seals tissue label", "AP SEALS label");
		mappingExternalIds.put("seas block label", "AP SEALS label");
		mappingExternalIds.put("seasl block label", "AP SEALS label");
		mappingExternalIds.put("seasl slide label", "AP SEALS label");
		mappingExternalIds.put("seasls slide label", "AP SEALS label");
		mappingExternalIds.put("auckland hospital molecular genetics lab", "Auckland Hospital");
		mappingExternalIds.put("block text", "Block label");
		mappingExternalIds.put("familial cancer refernce", "Familial Cancer reference");
		mappingExternalIds.put("imvs", "IMVS Lab Number");
		mappingExternalIds.put("imvs lab #", "IMVS Lab Number");
		mappingExternalIds.put("imvs lab#", "IMVS Lab Number");
		mappingExternalIds.put("label on tube from mt sinai ontario", "Mt Sinai Ontario");
		mappingExternalIds.put("peter mac ref", "Peter Mac Label");
		mappingExternalIds.put("pow mrn", "POWH MRN");
		mappingExternalIds.put("qmir person_iD", "QIMR Person_ID");
		mappingExternalIds.put("sa pathology", "SA Pathology");
	}

	private static final String GET_EXTERNAL_ID_DETAIL = "select * from CATISSUE_EXTERNAL_IDENTIFIER"; 
	
	private static final String DELETE_MIGRATED_DATA = "delete from CATISSUE_EXTERNAL_IDENTIFIER where specimen_id in (%s)";
	
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
			String key = externalId.toLowerCase().trim();
			externalId = mappingExternalIds.containsKey(key) ? mappingExternalIds.get(key) : externalId;
			
			Map<String, String> value = new HashMap<String, String>();
			value.put("externalID", externalId);
			value.put("externalValue", externalValue);
			
			externalIds.add(value);
		}
	}
}