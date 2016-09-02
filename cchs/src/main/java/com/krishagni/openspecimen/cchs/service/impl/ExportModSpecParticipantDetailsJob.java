package com.krishagni.openspecimen.cchs.service.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.openspecimen.cchs.dao.ModSpecParticpantDao;
import com.krishagni.openspecimen.cchs.events.ModSpecParticipantDetail;

@Configurable
public class ExportModSpecParticipantDetailsJob implements ScheduledTask {

	@Autowired
	private DaoFactory daoFactory;

	@Autowired
	private ModSpecParticpantDao specModDao;

	private ConfigUtil cfg = ConfigUtil.getInstance();

	@PlusTransactional
	public void doJob(ScheduledJobRun scheduledJobRun) throws Exception {
		saveModSpecParticipantDetails(scheduledJobRun);
	}

	private void saveModSpecParticipantDetails(ScheduledJobRun scheduledJobRun) {
		Long id = scheduledJobRun.getScheduledJob().getId();
		Date startDate = specModDao.getLastJobRunDate(id);
		Date endDate = scheduledJobRun.getStartedAt();
		List<ModSpecParticipantDetail> modSpecParticipantDetailsList = specModDao.getModSpecParticipantDetails(startDate,endDate);
		String dataDir = cfg.getDataDir();
		dataDir = ConfigUtil.getInstance().getStrSetting(CCHS_MODULE, EXPORT_DIR, dataDir);
		sendToPrint(modSpecParticipantDetailsList, dataDir, endDate);
	}

	private void sendToPrint(List<ModSpecParticipantDetail> list, String path, Date date) {
		CsvWriter csvWriter = null;
		try {
			String dateForCsvName = DateFormatUtils.format(date,"yyyyMMdd");
			File tempFile = new File(path,"CaTissue"+dateForCsvName+".csv");
			csvWriter = CsvFileWriter.createCsvFileWriter(tempFile);

			csvWriter.writeNext(new String[] {
			  msg("participant_id"),
			  msg("medical_rec_no"),
			  msg("last_name"),
			  msg("first_name"),
			  msg("middle_name"),
			  msg("birth_date"),
			  msg("gender"),
			  msg("specimen_id"),
			  msg("tissue_site"),
			  msg("collection_date")

			});

			for (ModSpecParticipantDetail detail: list) {
				csvWriter.writeNext(getRecord(detail));
			}

		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(csvWriter);
		}
	}

	private String msg(String key) {
		return MessageUtil.getInstance().getMessage(key);
	}

	private String[] getRecord(ModSpecParticipantDetail detail) {
		List<String> data = new ArrayList<String>();
		data.add(String.valueOf(detail.getParticipantId()));
		data.add(detail.getMedicalRecNo());
		data.add((detail.getLastName()));
		data.add(detail.getFirstName());
		data.add(detail.getMiddleName());
		if (detail.getBirthDate() != null) {
			data.add(Utility.getDateString(detail.getBirthDate()));
		}
		else {
			data.add(null);
		}

		data.add(detail.getGender());
		data.add(String.valueOf(detail.getSpecimenId()));
		data.add(detail.getTissueSite());

		if (detail.getCollectionDate() != null) {
			data.add(Utility.getDateString(detail.getCollectionDate()));
		}
		else {
			data.add(null);
		}

		return data.toArray(new String[0]);
	}

	private final String CCHS_MODULE = "cchs";

	private final String EXPORT_DIR = "export_dir";
}