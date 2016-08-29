package com.krishagni.openspecimen.cchs.service.impl;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.openspecimen.cchs.dao.SpecModByParticpantDao;
import com.krishagni.openspecimen.cchs.events.SpecModByParticpantDetail;

@Configurable
public class SpecModByParticipantJob implements ScheduledTask {
	@Autowired
	private DaoFactory daoFactory;

	@Autowired
	private SpecModByParticpantDao exportDao;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setExportDao(SpecModByParticpantDao exportDao) {
		this.exportDao = exportDao;
	}

	private ConfigUtil cfg= ConfigUtil.getInstance();

	@Override
	@PlusTransactional
	public void doJob(ScheduledJobRun scheduledJobRun) throws Exception {
		Long id=scheduledJobRun.getScheduledJob().getId();
		Date startDate = exportDao.getDateFromJob(id);
		Date endDate = scheduledJobRun.getStartedAt();

		getExportDetail(startDate,endDate);
	}

	private ResponseEvent<File> getExportDetail(Date startDate, Date endDate) {
		List<SpecModByParticpantDetail> exportDetailsList= exportDao.getSpecModByParticipantDetails(startDate,endDate);
		String dataDir =cfg.getDataDir();
		File file=sendToPrint(exportDetailsList, dataDir, endDate);

		return ResponseEvent.response(file);
	}

	private File sendToPrint(List<SpecModByParticpantDetail> list, String path, Date date) {
		CsvWriter csvWriter = null;

		try {
			String dateForCsvName= DateFormatUtils.format(date,"yyyyMMddHHmm");
			File tempFile = new File(path,"export-details-record"+dateForCsvName+".csv");
			csvWriter = CsvFileWriter.createCsvFileWriter(tempFile);

			csvWriter.writeNext(new String[] {
			  msg("specimen_id"),
			  msg("participant_id"),
			  msg("first_name"),
			  msg("middle_name"),
			  msg("last_name"),
			  msg("birth_date"),
			  msg("medical_rec_no"),
			  msg("tissue_site"),
			  msg("created_on"),
			  msg("gender")
			});

			for (SpecModByParticpantDetail detail: list) {
				csvWriter.writeNext(getRecord(detail));
			}

			return tempFile;
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(csvWriter);
		}
	}

	private String msg(String key) {
		return MessageUtil.getInstance().getMessage(key);
	}

	private String[] getRecord(SpecModByParticpantDetail detail) {
		List<String> data = new ArrayList<String>();
		data.add(String.valueOf(detail.getSpecimenId()));
		data.add(String.valueOf(detail.getParticipantId()));
		data.add(detail.getFirstName());
		data.add(detail.getMiddleName());
		data.add((detail.getLastName()));
		data.add(String.valueOf(detail.getBirthDate()));
		data.add(detail.getMedRecNo());
		data.add(detail.getTissueSite());
		data.add(String.valueOf(detail.getCollectionDate()));
		data.add(detail.getGender());

		return data.toArray(new String[0]);
	}
}