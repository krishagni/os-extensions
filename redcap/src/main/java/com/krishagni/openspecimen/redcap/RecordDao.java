package com.krishagni.openspecimen.redcap;

import com.krishagni.catissueplus.core.common.repository.Dao;

public interface RecordDao extends Dao<Record> {
	public Record getByRecordId(Long projectId, String recordId);
}
