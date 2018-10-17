package com.krishagni.openspecimen.msk2.repository;

import com.krishagni.catissueplus.core.common.repository.Dao;
import com.krishagni.openspecimen.msk2.domain.CarsStudyImportJob;

public interface CarsStudyImportJobDao extends Dao<CarsStudyImportJob> {
	CarsStudyImportJob getLatestJob();
}
