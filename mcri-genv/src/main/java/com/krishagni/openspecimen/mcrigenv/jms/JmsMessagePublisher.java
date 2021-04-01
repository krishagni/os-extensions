package com.krishagni.openspecimen.mcrigenv.jms;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jms.core.JmsTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class JmsMessagePublisher {
	private static final Map<String, JmsTemplate> jmsConnectionsMap = new ConcurrentHashMap<>();
	
	public void processMessage(SpecimenSavedEvent event) {
		String msg = toJSON(SpecimenDetail.from(event.getEventData(),false,false,true));
		publishMessage(msg);
	}
	
	private String toJSON(SpecimenDetail specimenDetail) {
		Map<String,Object> objectMap = new HashMap<String, Object>();
		objectMap.put("creationTime", specimenDetail.getCreatedOn().getTime());
		objectMap.put("user", specimenDetail.getCreatedBy());
		objectMap.put("specimen", specimenDetail);
		String msg = "{}";
		
		try {
			 msg = new ObjectMapper().writeValueAsString(objectMap);
		} catch (Exception e) {
			throw new RuntimeException("Error creating JSON message: " + e.getMessage(), e);
		}
		
		return msg;
	}
	
	private boolean publishMessage(String msg) {
		String name  = "java:comp/env/jms/connectionFactory";
		String queue = "mcri-genv-queue";
		if (StringUtils.isBlank(name) || StringUtils.isBlank(queue)) {
			throw OpenSpecimenException.userError(JmsFreezerErrorCode.CONN_FACTORY_OR_Q_NS);
		}

		JmsTemplate connection = getConnection(name);
		connection.send(queue, session -> session.createTextMessage(msg));
		return true;
	}

	private JmsTemplate getConnection(String name) {
		JmsTemplate connection = jmsConnectionsMap.get(name);
		if (connection != null) {
			return connection;
		}

		synchronized (JmsMessagePublisher.class) {
			connection = jmsConnectionsMap.get(name);
			if (connection != null) {
				return connection;
			}

			try {
				Context ctxt = new InitialContext();
				ConnectionFactory factory = (ConnectionFactory) ctxt.lookup(name);
				connection = new JmsTemplate(factory);
				jmsConnectionsMap.put(name, connection);
				return connection;
			} catch (Exception e) {
				throw new RuntimeException("Error connecting to the JMS queue: " + e.getMessage(), e);
			}
		}
	}
}
