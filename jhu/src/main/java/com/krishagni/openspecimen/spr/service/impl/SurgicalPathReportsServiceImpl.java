package com.krishagni.openspecimen.spr.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.DaoFactoryImpl;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.openspecimen.spr.events.LabHeader;
import com.krishagni.openspecimen.spr.events.LabResult;
import com.krishagni.openspecimen.spr.events.SprCrit;
import com.krishagni.openspecimen.spr.events.SprDetailCrit;
import com.krishagni.openspecimen.spr.events.SprReport;
import com.krishagni.openspecimen.spr.service.ClinicalApiConnection;
import com.krishagni.openspecimen.spr.service.SurgicalPathReportsService;
import com.krishagni.openspecimen.spr.util.ClinicalApiConnectionException;

public class SurgicalPathReportsServiceImpl implements SurgicalPathReportsService{

	private ClinicalApiConnection clincalApi;
	
	private DaoFactory daoFactory;
	
	public void setClincalApi(ClinicalApiConnection clincalApi) {
		this.clincalApi = clincalApi;
	}
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<LabHeader>> getReports(RequestEvent<SprCrit> req) {
		SprCrit crit = req.getPayload();
		List<LabHeader> result = new ArrayList<LabHeader>();
		if(StringUtils.isBlank(crit.getMrn())){
			return ResponseEvent.response(result);
		}
		
		try{
		  String facility = ConfigUtil.getInstance().getStrSetting("spr", "facilityName", "JHH");
		  Date loDate = new GregorianCalendar(1970,11-1,1).getTime(); 
		  Date hiDate = new Date();
		  insertAudit(crit.getMrn(), "VIEW_LIST", "", "T9930");
		  LabHeader[] results = clincalApi.GetLabs(crit.getMrn(), facility, loDate, hiDate, new String[]{"Anatomic Pathology"}, new String[]{"T9930"});
		  result = new ArrayList<LabHeader>(Arrays.asList(results));
		} catch (ClinicalApiConnectionException e){
			ResponseEvent.serverError(e);
		}
		return ResponseEvent.response(result);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SprReport> getReportDetails(RequestEvent<SprDetailCrit> req) {
		SprDetailCrit crit = req.getPayload();
		
		LabResult result = clincalApi.GetLabResult(crit.getMrn(), crit.getPathId());
		SprReport report = new SprReport();
		report.setCollectionDate(result.getCollectionDateTime());
		report.setName(result.getTestShortDesc());
		report.setSpecimenId(crit.getPathId());
		report.setMrn(crit.getMrn());
		report.setText(result.getComponents()[0].getResult());
		report.setPathId(result.getSpecimenId());
		insertAudit(crit.getMrn(), "VIEW_DETAIL", crit.getPathId(), result.getTestCode());
		return ResponseEvent.response(report);
	}
	
	private void insertAudit(String mrn, String operation, String pathId, String testCode) {
		String ipAddress = AuthUtil.getRemoteAddr();
		String loginName = AuthUtil.getCurrentUser().getLoginName();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		String currentDate = dateFormat.format(new Date());
		Query query = ((DaoFactoryImpl)daoFactory).getSessionFactory()
				.getCurrentSession()
				.createSQLQuery(INSERT_AUDIT_SQL);
		query.setParameter(0, currentDate);
		query.setParameter(1, ipAddress);
		query.setParameter(2, loginName);
		query.setParameter(3, operation);
		query.setParameter(4, mrn);
		query.setParameter(5, pathId);
		query.setParameter(6, testCode);
		query.executeUpdate();
	}

	private final String INSERT_AUDIT_SQL =
			"insert into SPR_AUDIT_DETAILS (access_date, ip_address, login_name, operation, mrn, path_id, test_code)" + 
					"values (?,?,?,?,?,?,?)";

}
