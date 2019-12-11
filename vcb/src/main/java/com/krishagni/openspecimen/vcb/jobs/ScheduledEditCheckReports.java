package com.krishagni.openspecimen.vcb.jobs;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.exporter.events.ExportDetail;
import com.krishagni.catissueplus.core.exporter.events.ExportJobDetail;
import com.krishagni.catissueplus.core.exporter.services.ExportService;

@Configurable
public class ScheduledEditCheckReports implements ScheduledTask {
	private static final String QC_ERROR_LOG = "qcErrorLog";

	private final static String VCB_SCHEDULED_ERROR_REPORTS = "vcb_scheduled_error_reports";

	@Autowired
	private ExportService exportSvc;

	@Autowired
	private DaoFactory daoFactory;

	@Override
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		ExportJobDetail exportJobDetail = exportSvc.exportObjects(RequestEvent.wrap(getExportDetail(jobRun))).getPayload();

		notifyUsers(exportJobDetail, jobRun);
	}

	private ExportDetail getExportDetail(ScheduledJobRun jobRun) {
		ExportDetail obj = new ExportDetail();
		Map<String, String> params = new HashMap<>();
		String cpId = jobRun.getRtArgs();
		params.put("cpId", cpId);

		Long endDate = new Date().getTime();
		params.put("endDate", String.valueOf(endDate));

		Long startDate = getLastJobRun(jobRun.getScheduledJob().getName()).getTime();
		params.put("startDate", String.valueOf(startDate));

		obj.setObjectType(QC_ERROR_LOG);
		obj.setParams(params);

		return obj;
	}

	@PlusTransactional
 	public Date getLastJobRun(String name) {
 		ScheduledJob job = daoFactory.getScheduledJobDao().getJobByName(name);
 		List<Long> jobIds = Collections.singletonList(job.getId());

 		Map<Long, Date> jobLastRun = daoFactory.getScheduledJobDao().getJobsLastRunTime(jobIds, Collections.singletonList("SUCCEEDED"));
 		return jobLastRun.getOrDefault(job.getId(), null);
 	}

	private void notifyUsers(ExportJobDetail exportJobDetail, ScheduledJobRun jobRun) {
		String date = Utility.getDateString(jobRun.getCreationTime());
		String cpIds = exportJobDetail.getParams().entrySet().stream()
							.filter(e -> e.getKey().equals("cpId"))
							.map(Map.Entry::getValue).collect(Collectors.joining(", "));

		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("$subject", new String[] { date });
		emailProps.put("date", date);
		emailProps.put("ccAdmin", true);
		emailProps.put("cps", cpIds);

		File[] attachments = new File[] { getOutputFile(exportJobDetail) };
		for (User user : getNotifUsers()) {
			emailProps.put("rcpt", user);
			EmailUtil.getInstance().sendEmail(VCB_SCHEDULED_ERROR_REPORTS, new String[] {user.getEmailAddress()}, attachments, emailProps);
		}
	}

	private File getOutputFile(ExportJobDetail exportJobDetail) {
		String dataDir = ConfigUtil.getInstance().getDataDir();
		char separatorChar = File.separatorChar;

		StringBuilder exportJobsDir = new StringBuilder(dataDir).append(separatorChar)
				.append("export-jobs").append(separatorChar)
				.append(String.valueOf(exportJobDetail.getId()));

		return new File(exportJobsDir.toString(), "output.zip");
	}

	@PlusTransactional
	private List<User> getNotifUsers() {
		List<User> systemAdmins = daoFactory.getUserDao().getSuperAndInstituteAdmins(null);

		String itAdminEmailId = ConfigUtil.getInstance().getItAdminEmailId();
		if (StringUtils.isNotBlank(itAdminEmailId)) {
			User itAdmin = new User();
			itAdmin.setFirstName("IT");
			itAdmin.setLastName("Admin");
			itAdmin.setEmailAddress(itAdminEmailId);
			systemAdmins.add(itAdmin);
		}

		return systemAdmins;
	}
}
