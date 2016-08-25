package com.krishagni.openspecimen.cchs.dao.impl;

import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.common.repository.Dao;
import com.krishagni.openspecimen.cchs.events.SpecModByParticpantDetail;

public interface SpecModByParticpantDao extends Dao<SpecModByParticpantDetail> {
	public List<SpecModByParticpantDetail> getExportDetails(Date startDate, Date endDate);

	public Date getDate(Long id);
}
