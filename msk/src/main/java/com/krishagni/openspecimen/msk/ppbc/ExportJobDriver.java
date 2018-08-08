package com.krishagni.openspecimen.msk.ppbc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

public class ExportJobDriver implements ScheduledTask {

	@Override
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		if (!getExportFolder().exists()) {
			getExportFolder().mkdir();
		}
		
		getExportSubFolder().mkdirs();
		
		new DistributionProtocolExport().doJob(jobRun);
		new ParticipantExport().doJob(jobRun);
	}

	private File getExportSubFolder() {
		String folderName = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
		return new File(getExportFolder(), folderName);
	}
	
	private File getExportFolder() {
		return new File(ConfigUtil.getInstance().getDataDir() + File.separatorChar + "MskExportFolder");
	}
}
