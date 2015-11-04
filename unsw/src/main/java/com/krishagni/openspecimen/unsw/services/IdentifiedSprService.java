package com.krishagni.openspecimen.unsw.services;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.FileDetail;
import com.krishagni.openspecimen.unsw.events.IdentifiedSprDetail;

public interface IdentifiedSprService {
	
	public ResponseEvent<String> getIdentifiedSprName(RequestEvent<Long> req);
	
	public ResponseEvent<FileDetail> getIdentifiedSpr(RequestEvent<Long> req);
	
	public ResponseEvent<FileDetail> uploadIdentifiedSpr(RequestEvent<IdentifiedSprDetail> req);

	public ResponseEvent<Boolean> deleteIdentifiedSpr(RequestEvent<Long> req);
}
