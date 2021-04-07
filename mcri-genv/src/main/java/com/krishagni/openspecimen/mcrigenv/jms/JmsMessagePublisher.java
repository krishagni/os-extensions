package com.krishagni.openspecimen.mcrigenv.jms;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.biospecimen.domain.*;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jms.core.JmsTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class JmsMessagePublisher {
	private static final Map<String, JmsTemplate> jmsConnectionsMap = new ConcurrentHashMap<>();

	public void processMessage(Specimen specimen) {
		SpecimenDetail specimenDetail = SpecimenDetail.from(specimen,false,false,true);
		String msg = toJSON(specimenDetail);
		publishMessage(msg);
		checkSpecimenQuality(specimen);
	}

	private void checkSpecimenQuality(Specimen specimen){

		if(!specimen.isPrimary()){
			return;
		}

		SpecimenReceivedEvent recvEvent = specimen.getReceivedEvent();
		if ((!recvEvent.getQuality().getValue().equals("Damaged")) && (!recvEvent.getQuality().getValue().equals("Unacceptable, Not Specified"))) {
			return;
		}

		Visit visit = specimen.getVisit();
		Site site = visit.getSite();
		CollectionProtocolRegistration cpr = visit.getRegistration();
		CollectionProtocol cp = cpr.getCollectionProtocol();

		Map<String,Object> props = new HashMap<>();
		props.put("$subject",new String[] {recvEvent.getQuality().getValue()});
		props.put("site",visit.getSite().getName());
		props.put("type",specimen.getSpecimenType().getValue());
		props.put("ppid",cpr.getPpid());
		props.put("cpShortTitle",cp.getShortTitle());
		props.put("id",specimen.getId());
		props.put("quality",recvEvent.getQuality().getValue());
		EmailUtil.getInstance().sendEmail("specimen_sample_quality_report", new String[] {"nikhil@krishagni.com"},null,props);
	}

	private String toJSON(SpecimenDetail specimenDetail) {
		FilterProvider filters = new SimpleFilterProvider()
				.addFilter("withoutId", SimpleBeanPropertyFilter.serializeAllExcept("id", "statementId"));
		String msg = "{}";
		
		try {
			 msg =  new ObjectMapper()
					.setSerializationInclusion(JsonInclude.Include.NON_NULL)
					.writer(filters)
					.writeValueAsString(specimenDetail);
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
