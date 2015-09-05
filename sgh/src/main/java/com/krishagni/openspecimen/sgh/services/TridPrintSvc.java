package com.krishagni.openspecimen.sgh.services;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.openspecimen.sgh.events.BulkTridPrintOpDetail;
import com.krishagni.openspecimen.sgh.events.TridsRePrintOpDetail;

public interface TridPrintSvc {
	public ResponseEvent<Boolean> generateAndPrintTrids(RequestEvent<BulkTridPrintOpDetail> req);
	
	public ResponseEvent<Boolean> printTrids(RequestEvent<TridsRePrintOpDetail> req);

}
