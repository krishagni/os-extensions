package com.krishagni.openspecimen.demo.services;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.openspecimen.demo.events.ClientDetail;

public interface ClientService {
	public ResponseEvent<ClientDetail> createUser (RequestEvent<ClientDetail> req);
}