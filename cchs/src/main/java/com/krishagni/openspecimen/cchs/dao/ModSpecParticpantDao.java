package com.krishagni.openspecimen.cchs.dao;

import java.util.Date;
import java.util.List;

import com.krishagni.openspecimen.cchs.events.ModSpecParticipantDetail;

public interface ModSpecParticpantDao {

	public List<ModSpecParticipantDetail> getModSpecParticipantDetails(Date startDate, Date endDate);

	public Date getLastJobRunDate(Long id);
}
