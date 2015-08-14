package com.krishagni.openspecimen.sgh.services;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.openspecimen.sgh.events.BulkParticipantRegDetail;
import com.krishagni.openspecimen.sgh.events.BulkParticipantRegSummary;


public interface CprService {
	public ResponseEvent<BulkParticipantRegDetail> registerParticipants(RequestEvent<BulkParticipantRegSummary> req);
}

