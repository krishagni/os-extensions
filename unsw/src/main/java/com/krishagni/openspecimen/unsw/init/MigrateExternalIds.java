package com.krishagni.openspecimen.unsw.init;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
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
	
	private static Map<String, String> mappingExternalIds = new HashMap<String, String>();

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
				String adminEmailId = "admin@unsw.edu.au"; //"admin@admin.com";
				String adminDomain = "openspecimen";
				User user = daoFactory.getUserDao().getUser(adminEmailId, adminDomain);
				UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user,null, user.getAuthorities());
				SecurityContextHolder.getContext().setAuthentication(token);
				
				final List<ExternalIdDetail> externalIds = getExternalIdsDetail(GET_EXTERNAL_ID_DETAIL);
				
				Map<Long, List<Map<String, String>>> extensionDetail = generateSpecimenExtensionDetail(externalIds);
				for (Long specimenId : extensionDetail.keySet()) {
					ExtensionDetail detail = new ExtensionDetail();
					
					AttrDetail attr = new AttrDetail();
					attr.setName("externalIDs");
					attr.setValue(extensionDetail.get(specimenId));
					detail.setAttrs(Collections.singletonList(attr));
					
					Specimen specimen = daoFactory.getSpecimenDao().getById(specimenId);
					specimen.setExtension(DeObject.createExtension(detail, specimen));
					specimen.addOrUpdateExtension();
					
					deleteMigratedData(specimenId);
				}
				
				logger.info("Migration completed successfully...");
				System.out.println("Migration completed successfully...");
				return 1;
			}
		});
	}
	
	private List<ExternalIdDetail> getExternalIdsDetail(String getExternalIdDetail) {
		return jdbcTemplate.query(getExternalIdDetail, 
			new RowMapper<ExternalIdDetail>() {
				public ExternalIdDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
					String name = rs.getString("name").toLowerCase().trim();
					String externalId = mappingExternalIds.containsKey(name) ? mappingExternalIds.get(name) : rs.getString("name");
					
					ExternalIdDetail detail = new ExternalIdDetail();
					detail.setExternalId(externalId);
					detail.setExternalValue(rs.getString("value"));
					detail.setSpecimenId(rs.getLong("specimen_id"));
					return detail;
				}
			}
		);
	}
	
	private Map<Long, List<Map<String, String>>> generateSpecimenExtensionDetail(List<ExternalIdDetail> externalIds) {
		Map<Long, List<Map<String, String>>> extensionDetail = new HashMap<Long, List<Map<String, String>>>();
		for (ExternalIdDetail externalIdDetail : externalIds) {
			Long specimenId = externalIdDetail.getSpecimenId();
			String externalId = externalIdDetail.getExternalId();
			String externalValue = externalIdDetail.getExternalValue();
			
			List<Map<String, String>> externalIdList = extensionDetail.get(specimenId);
			if (externalIdList == null) {
				externalIdList = new ArrayList<Map<String,String>>();
			}
			
			Map<String, String> obj = new HashMap<String, String>();
			obj.put("externalID", externalId);
			obj.put("externalValue", externalValue);
				
			externalIdList.add(obj);
			extensionDetail.put(specimenId, externalIdList);
		}
		return extensionDetail;
	}
	
	private void deleteMigratedData(Long id) {
		jdbcTemplate.update(String.format(DELETE_MIGRATED_DATA, id));
		logger.info("Deleted record for specimen id : " + id);
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
	
	private static final String DELETE_MIGRATED_DATA = "delete from CATISSUE_EXTERNAL_IDENTIFIER where specimen_id = %d";
	
	private class ExternalIdDetail {
		private String externalId;
		
		private String externalValue;
		
		private Long specimenId;

		public String getExternalId() {
			return externalId;
		}

		public void setExternalId(String externalId) {
			this.externalId = externalId;
		}

		public String getExternalValue() {
			return externalValue;
		}

		public void setExternalValue(String externalValue) {
			this.externalValue = externalValue;
		}

		public Long getSpecimenId() {
			return specimenId;
		}

		public void setSpecimenId(Long specimenId) {
			this.specimenId = specimenId;
		}
	}
}