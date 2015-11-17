package com.krishagni.openspecimen.redcap;

import java.util.List;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class RecordDaoImpl extends AbstractDao<Record> implements RecordDao {

	@SuppressWarnings("unchecked")
	@Override
	public Record getByRecordId(Long projectId, String recordId) {
		List<Record> records = sessionFactory.getCurrentSession()
			.getNamedQuery(GET_BY_RECORD_ID)
			.setLong("projectId", projectId)
			.setString("recordId", recordId)
			.list();
		return records.isEmpty() ? null : records.iterator().next();
	}
	
	private static final String FQN = Record.class.getName();
	
	private static final String GET_BY_RECORD_ID = FQN + ".getByRecordId";
}