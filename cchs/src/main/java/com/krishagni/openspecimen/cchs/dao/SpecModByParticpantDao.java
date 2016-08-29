package com.krishagni.openspecimen.cchs.dao;

import java.util.Date;
import java.util.List;

import com.krishagni.openspecimen.cchs.events.SpecModByParticpantDetail;

public interface SpecModByParticpantDao {
	public List<SpecModByParticpantDetail> getSpecModByParticipantDetails(Date startDate, Date endDate);

	public Date getDateFromJob(Long id);
}
