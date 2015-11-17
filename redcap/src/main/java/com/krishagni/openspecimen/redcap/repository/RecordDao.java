package com.krishagni.openspecimen.redcap.repository;

import com.krishagni.catissueplus.core.common.repository.Dao;
import com.krishagni.openspecimen.redcap.domain.Record;

public interface RecordDao extends Dao<Record> {
	public Record getByRecordId(Long projectId, String recordId);
}
