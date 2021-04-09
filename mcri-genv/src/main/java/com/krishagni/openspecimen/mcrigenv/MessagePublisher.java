package com.krishagni.openspecimen.mcrigenv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jms.core.JmsTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class MessagePublisher {
	private static final Map<String, JmsTemplate> jmsConnectionsMap = new ConcurrentHashMap<>();

	public void publish(String connFactory, String queue, Specimen specimen) {
		if (StringUtils.isBlank(connFactory) || StringUtils.isBlank(queue)) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "JMS queue connection factory or name is not specified");
		}

		SpecimenDetail detail = SpecimenDetail.from(specimen, false, false, true);
		publishMessage(connFactory, queue, toJson(detail));
	}

	private String toJson(SpecimenDetail specimenDetail) {
		try {
			FilterProvider filters = new SimpleFilterProvider()
				.addFilter("withoutId", SimpleBeanPropertyFilter.serializeAllExcept("id", "statementId"));
			return new ObjectMapper()
				.setSerializationInclusion(JsonInclude.Include.NON_NULL)
				.writer(filters)
				.writeValueAsString(specimenDetail);
		} catch (Exception e) {
			throw new RuntimeException("Error creating JSON message: " + e.getMessage(), e);
		}
	}

	private void publishMessage(String connFactory, String queueName, String msg) {
		getConnection(connFactory).send(queueName, session -> session.createTextMessage(msg));
	}

	private JmsTemplate getConnection(String connFactory) {
		JmsTemplate connection = jmsConnectionsMap.get(connFactory);
		if (connection != null) {
			return connection;
		}

		synchronized (MessagePublisher.class) {
			connection = jmsConnectionsMap.get(connFactory);
			if (connection != null) {
				return connection;
			}

			try {
				Context ctxt = new InitialContext();
				ConnectionFactory factory = (ConnectionFactory) ctxt.lookup(connFactory);
				connection = new JmsTemplate(factory);
				jmsConnectionsMap.put(connFactory, connection);
				return connection;
			} catch (Exception e) {
				throw new RuntimeException("Error connecting to the JMS queue: " + e.getMessage(), e);
			}
		}
	}
}
