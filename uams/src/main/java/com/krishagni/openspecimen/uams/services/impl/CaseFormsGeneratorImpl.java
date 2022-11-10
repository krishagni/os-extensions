package com.krishagni.openspecimen.uams.services.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PdfUtil;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.TemplateService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.openspecimen.uams.services.CaseFormsGenerator;

public class CaseFormsGeneratorImpl implements CaseFormsGenerator {

	private static final LogUtil logger = LogUtil.getLogger(CaseFormsGeneratorImpl.class);

	private static final String MODULE = "biospecimen";

	private static final String CASE_FORM_TMPL = "uams_case_form_tmpl";

	private static final String NOTIF_TMPL = "case_forms_generated";

	private static final String DEF_TMPL = "com/krishagni/openspecimen/uams/case_forms_tmpl.html";

	private TemplateService tmplSvc;

	private DaoFactory daoFactory;

	private ExportService exportSvc;

	private ThreadPoolTaskExecutor taskExecutor;

	public void setTmplSvc(TemplateService tmplSvc) {
		this.tmplSvc = tmplSvc;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<File> generateCaseForms(RequestEvent<Map<String, Object>> req) {
		try {
			Map<String, Object> input = req.getPayload();
			Number count = (Number) input.get("count");
			List<String> subjectIds = (List<String>) input.get("subjectIds");

			ExportCaseForms task = null;
			if (count != null) {
				task = new ExportCaseForms(count.intValue());
			} else if (subjectIds != null && !subjectIds.isEmpty()) {
				task = new ExportCaseForms(subjectIds);
			}

			Future<File> result = taskExecutor.submit(task);
			File output = result.get(30 * 1000L, TimeUnit.MILLISECONDS);
			return ResponseEvent.response(output);
		} catch (TimeoutException e) {
			return ResponseEvent.response(null);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public ResponseEvent<File> getFile(RequestEvent<String> req) {
		try {
			File caseFormsDir = new File(ConfigUtil.getInstance().getDataDir(), "case-forms");
			File caseForms    = new File(caseFormsDir, req.getPayload());
			if (!caseForms.getAbsolutePath().equals(caseForms.getCanonicalPath()) || !caseForms.exists()) {
				throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Invalid fileId: " + req.getPayload());
			}

			return ResponseEvent.response(caseForms);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private class ExportCaseForms implements Callable<File> {

		private User user;

		private String ipAddress;

		private int count;

		private List<String> subjectIds;

		private List<String> caseIds;

		ExportCaseForms(int count) {
			this.count = count;
			this.user = AuthUtil.getCurrentUser();
			this.ipAddress = AuthUtil.getRemoteAddr();
		}

		ExportCaseForms(List<String> subjectIds) {
			this.subjectIds = subjectIds;
			this.user = AuthUtil.getCurrentUser();
			this.ipAddress = AuthUtil.getRemoteAddr();
		}

		@Override
		@PlusTransactional
		public File call() throws Exception {
			Date startTime = Calendar.getInstance().getTime();
			File caseFormsDir = new File(ConfigUtil.getInstance().getDataDir(), "case-forms");
			if (!caseFormsDir.exists()) {
				caseFormsDir.mkdirs();
			}

			String fileId = UUID.randomUUID().toString();

			String tmpl = DEF_TMPL;
			File tmplFile = ConfigUtil.getInstance().getFileSetting(MODULE, CASE_FORM_TMPL, null);
			if (tmplFile != null) {
				tmpl = tmplFile.getAbsolutePath();
			}

			List<File> pdfs = new ArrayList<>();
			try {
				AuthUtil.setCurrentUser(user, ipAddress);
				if ((subjectIds == null || subjectIds.isEmpty()) && count <= 0) {
					throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Specify either the subject IDs or count of case forms to print.");
				} else if (count > 0) {
					Long subjectStartId = daoFactory.getUniqueIdGenerator().getUniqueId("UAMS_SUBJECT_ID", "ALL", 0L, count);
					subjectIds = new ArrayList<>();
					caseIds = new ArrayList<>();
					for (int i = 0; i < count; ++i) {
						subjectIds.add(String.format("AI%010d", (subjectStartId + i)));
						caseIds.add(String.format("CI%010d", (subjectStartId + i)));
					}
				} else {
					subjectIds = subjectIds.stream().distinct().map(String::toUpperCase).collect(Collectors.toList());
					caseIds = new ArrayList<>();
					Long caseStartId = daoFactory.getUniqueIdGenerator().getUniqueId("UAMS_CASE_ID", "ALL", 0L, subjectIds.size());
					for (int i = 0; i < subjectIds.size(); ++i) {
						caseIds.add(String.format("CI7%09d", (caseStartId + i)));
					}
				}

				for (int i = 0; i < subjectIds.size(); ++i) {
					String subjectId = subjectIds.get(i);
					String caseId    = caseIds.get(i);

					Map<String, Object> props = new HashMap<>();
					props.put("subjectId", subjectId);
					props.put("caseId", caseId);

					File pdf = new File(caseFormsDir, fileId + "_" + i + ".pdf");
					PdfUtil.getInstance().toPdf(tmpl, props, pdf);
					pdfs.add(pdf);
				}

				File outputPdf = new File(caseFormsDir, fileId + ".pdf");
				try (FileOutputStream pdfOut = new FileOutputStream(outputPdf)) {
					PDFMergerUtility utility = new PDFMergerUtility();
					for (File pdf : pdfs) {
						utility.addSource(pdf);
					}
					utility.setDestinationStream(pdfOut);
					utility.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());

					Map<String, String> params = new HashMap<>();
					params.put("count", String.valueOf(count));
					params.put("subjectIds", Utility.stringListToCsv(subjectIds));
					params.put("output", outputPdf.getName());
					exportSvc.saveJob("uams_case_forms", startTime, params);

					List<User> rcpts = daoFactory.getUserDao().getSuperAndInstituteAdmins(null);
					if (!rcpts.contains(user)) {
						rcpts.add(user);
					}

					Map<String, Object> props = new HashMap<>();
					props.put("fileId", outputPdf.getName());
					props.put("user", user);
					for (User rcpt : rcpts) {
						props.put("rcpt", rcpt);
						EmailUtil.getInstance().sendEmail(NOTIF_TMPL, new String[] { rcpt.getEmailAddress() }, null, props);
					}

					return outputPdf;
				} catch (Exception e) {
					throw OpenSpecimenException.serverError(e);
				}
			} catch (Exception e) {
				logger.error("Error generating case forms: " + e.getMessage(), e);
				if (e instanceof OpenSpecimenException) {
					throw e;
				}

				throw OpenSpecimenException.serverError(e);
			} finally {
				AuthUtil.clearCurrentUser();
				pdfs.forEach(File::delete);
			}
		}
	}
}
