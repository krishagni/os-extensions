package com.krishagni.openspecimen.msk2.repository.impl;

import org.hibernate.criterion.Order;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.openspecimen.msk2.domain.CarsStudyImportJob;
import com.krishagni.openspecimen.msk2.repository.CarsStudyImportJobDao;

public class CarsStudyImportJobDaoImpl extends AbstractDao<CarsStudyImportJob> implements CarsStudyImportJobDao {
	@Override
	public CarsStudyImportJob getLatestJob() {
		return (CarsStudyImportJob) getCurrentSession().createCriteria(CarsStudyImportJob.class)
			.addOrder(Order.desc("endTime"))
			.setMaxResults(1)
			.uniqueResult();
	}
}
