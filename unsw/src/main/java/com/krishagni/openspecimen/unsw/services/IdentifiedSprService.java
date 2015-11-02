package com.krishagni.openspecimen.unsw.services;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.openspecimen.unsw.events.IdentifiedSprDetail;

public interface IdentifiedSprService {
	
	public ResponseEvent<IdentifiedSprDetail> getIdentifiedSprDetail(RequestEvent<Long> req);

}
