package com.krishagni.openspecimen.unsw.init;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.events.VisitDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.VisitService;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail.AttrDetail;
import com.krishagni.catissueplus.core.de.events.FileDetail;
import com.krishagni.catissueplus.core.de.services.FormService;

public class MigrateIdentifiedReports implements InitializingBean {

	private static Log logger = LogFactory.getLog(MigrateIdentifiedReports.class);

	private PlatformTransactionManager txnMgr;
	
	private FormService formSvc;
	
	private VisitService visitSvc;
	
	private DaoFactory daoFactory;
	
	private JdbcTemplate jdbcTemplate;
	
	public void setTxnMgr(PlatformTransactionManager txnMgr) {
		this.txnMgr = txnMgr;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	public void setFormSvc(FormService formSvc) {
		this.formSvc = formSvc;
	}
	
	public void setVisitSvc(VisitService visitSvc) {
		this.visitSvc = visitSvc;
	}
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		migrateFromDatabase();
	}
	
	private void migrateFromDatabase() {
		Date startTime = Calendar.getInstance().getTime();
		logger.info("Migration from static table start time: " + startTime);
		
		int totalMigratedReportsCnt = 0;
		final int maxResult = 1;
		boolean moreRecords = true;
		while (moreRecords) {
			TransactionTemplate txnTmpl = new TransactionTemplate(txnMgr);
			Integer migrtedReportsCnt = 
				txnTmpl.execute(new TransactionCallback<Integer>() {
					@Override
					public Integer doInTransaction(TransactionStatus trStatus) {
						final List<SprDetail> sprDetails  = getSprDetailsFromTable(getReportsSql(), maxResult);
						if (CollectionUtils.isEmpty(sprDetails)) {
							logger.info("No surgical pathology reports in static table to migrate from static table");
							return 0;
						}
						String adminEmailId = "admin@unsw.edu.au"; //"admin@admin.com";
						String adminDomain = "openspecimen";
						User user = daoFactory.getUserDao().getUser(adminEmailId, adminDomain);
						UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user,null, user.getAuthorities());
						SecurityContextHolder.getContext().setAuthentication(token);
						for (final SprDetail detail : sprDetails) {
							InputStream is = null;
							logger.info("Processing report with ID: " + detail.getReportId() + " for visit with name : " + detail.getVisitName());
							try {
								//Covert string to fileInput Stream
                is = new ByteArrayInputStream(detail.getReportContent().getBytes());
								
								MultipartFile multipartFile = new MockMultipartFile("file", "spr-report.txt", "text/plain", is);
								
								RequestEvent<MultipartFile> req = new RequestEvent<MultipartFile>();
								
								req.setPayload(multipartFile);
								logger.info("Uploading the file");
								ResponseEvent<FileDetail> response = formSvc.uploadFile(req);
								RequestEvent<EntityQueryCriteria> visitReq = new RequestEvent<EntityQueryCriteria>();
								visitReq.setPayload(new EntityQueryCriteria(detail.getVisitId()));
								
								logger.info("Retrieving Visit with Name: " + detail.getVisitName());
								ResponseEvent<VisitDetail> visitResponse = visitSvc.getVisit(visitReq);
								if(!visitResponse.isSuccessful()){
									logger.info("Error during migration for visit: " + detail.getVisitName() + "ID: " + detail.getVisitId());
				        }
								VisitDetail visit = visitResponse.getPayload();
								logger.info("Got the visit with Name : " + detail.getVisitName());
								
								String controlName = "fileUpload";
				        Map<String, String> map = new HashMap<String, String>();
				        map.put("filename", response.getPayload().getFilename()); // get from above file upload response
				        map.put("contentType", response.getPayload().getContentType()); // get from above file upload response
				        map.put("fileId", response.getPayload().getFileId()); // get from above file upload response
				        Object value = map;
				        List<AttrDetail> attrs = new ArrayList<AttrDetail>();
				        AttrDetail attr = new AttrDetail();
				        attr.setName(controlName);
				        attr.setValue(value);
				        attrs.add(attr);
				        
				        ExtensionDetail extDetail = new ExtensionDetail();
				        extDetail.setAttrs(attrs);
				        visit.setExtensionDetail(extDetail);
				        
				        visitResponse = visitSvc.updateVisit(new RequestEvent<VisitDetail>(visit));
				        if(!visitResponse.isSuccessful()){
				        	logger.info("Error during migration for visit: " + visit.getName() + "ID: " + visit.getId());
				        }
				        logger.info("Updated visit");
								jdbcTemplate.update(DISABLE_OLD_MIGRATED_SPR_SQL, detail.getReportId());
								logger.info("Updated the activity status for report with ID: " + detail.getReportId());
								
							} catch (Exception e) {
								logger.error("Error while migrating records from static table: ", e);
								e.printStackTrace();
							} finally {
								IOUtils.closeQuietly(is);
							}
						} 
						return sprDetails.size();
					}
				});
			
			totalMigratedReportsCnt = totalMigratedReportsCnt + migrtedReportsCnt;
			logger.info("Number of reports migrated from static table, till now: " + totalMigratedReportsCnt);
			if (migrtedReportsCnt < maxResult) {
				moreRecords = false;
			}
		}
		
		Date endTime = Calendar.getInstance().getTime();
		logger.info("Migration from static table end time: " + endTime);
		logger.info("Total time for migration from static table: " + 
				(endTime.getTime() - startTime.getTime()) / (1000 * 60) + " minutes");		
	}

	private String getReportsSql() {
		return GET_IDENTIFIED_SPR_DETAILS_SQL_ORACLE;
	}

	private List<SprDetail> getSprDetailsFromTable(final String getSprDetailsSql, final int maxResult) {
		return jdbcTemplate.query(
				getSprDetailsSql,
				new Object[] {maxResult},
				new RowMapper<SprDetail>() {
					public SprDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
						System.out.println("Got results");
						SprDetail sprDetail = new SprDetail();
						sprDetail.setVisitId(rs.getLong("visitId"));
						sprDetail.setVisitName(rs.getString("visitName"));
						sprDetail.setActivityStatus(rs.getString("activityStatus"));
						sprDetail.setReportContent(rs.getString("reportContent"));
						sprDetail.setReportId(rs.getLong("reportId"));
						return sprDetail;
					}
			 });					
	}
	
	private static final String GET_IDENTIFIED_SPR_DETAILS_SQL_ORACLE =
			"select "+ 
			  "pr.identifier reportId, pr.activity_status activityStatus, " +
			  "dr.scg_id visitId, scg.name visitName, rc.report_data reportContent " + 
			"from " +
			  "catissue_identified_report dr " + 
			  "join catissue_pathology_report pr on dr.identifier = pr.identifier " + 
			  "join catissue_report_textcontent rtc on rtc.report_id = pr.identifier " + 
			  "join catissue_report_content rc on rc.identifier = rtc.identifier " +
			  "join catissue_specimen_coll_group scg on dr.scg_id = scg.identifier " +
			"where " +
			  "pr.activity_status <> 'Disabled' and scg.activity_status <> 'Disabled' and rownum <= ?";
	
	private static final String DISABLE_OLD_MIGRATED_SPR_SQL = 
			"update " +
			  "catissue_pathology_report " +
			"set " +
			  "activity_status = 'Disabled' " +
			"where "+
			  "identifier = ?" ;
	
	private class SprDetail {
		private Long visitId;
		 
		private String visitName;
		 
		private Long reportId;
		 
		private String activityStatus;
		 
		private String reportContent;
		 
		public Long getVisitId() {
			return visitId;
		}

		public void setVisitId(Long visitId) {
			this.visitId = visitId;
		}

		public String getVisitName() {
			return visitName;
		}

		public void setVisitName(String visitName) {
			this.visitName = visitName;
		}

		public Long getReportId() {
			return reportId;
		}

		public void setReportId(Long reportId) {
			this.reportId = reportId;
		}

		public String getActivityStatus() {
			return activityStatus;
		}

		public void setActivityStatus(String activityStatus) {
			this.activityStatus = activityStatus;
		}

		public String getReportContent() {
			return reportContent;
		}

		public void setReportContent(String reportContent) {
			this.reportContent = reportContent;
		}
	}
}
