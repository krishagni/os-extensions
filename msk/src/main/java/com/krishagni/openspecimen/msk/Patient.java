package com.krishagni.openspecimen.msk;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

public class Patient {
	//
	// TODO: pick from config
	//
	private static final List<String> PROTOCOLS_WITH_Q = Arrays.asList("06-107");

	private static final Date CONSENT_WAIVED_UNTIL = april132003();

	private String mrn;

	private boolean alive;

	private Map<String, ProtocolConsent> protocolConsents = new LinkedHashMap<>();

	public String getMrn() {
		return mrn;
	}

	public void setMrn(String mrn) {
		this.mrn = mrn;
	}

	public boolean isAlive() {
		return alive;
	}

	public void setAlive(boolean alive) {
		this.alive = alive;
	}

	public Collection<ProtocolConsent> getProtocolConsents() {
		return protocolConsents.values();
	}

	public void addProtocolConsent(String protocol, String regId, Boolean consented, String question, String answer) {
		String key = protocol + ":" + regId;
		ProtocolConsent pc = protocolConsents.get(key);
		if (pc == null) {
			pc = new ProtocolConsent();
			pc.setProtocol(protocol);
			pc.setRegId(regId);
			pc.setConsented(consented);
			protocolConsents.put(key, pc);
		}

		pc.addResponse(question, answer);
	}


	public boolean isConsented(Date visitDate, Collection<String> questions) {
		if (!isAlive() && visitDate.before(CONSENT_WAIVED_UNTIL)) {
			return true;
		} else if (isAlive()) {
			for (ProtocolConsent consent : getProtocolConsents()) {
				if (consent.isConsentedToAny(questions)) {
					return true;
				}
			}
		}

		//
		// TODO: was patient ever approached for consent and declined?
		//
		return false;
	}

	private static Date april132003() {
		Calendar cal = Calendar.getInstance();
		cal.set(2013, Calendar.APRIL, 13, 0, 0, 0);
		return cal.getTime();
	}

	public class ProtocolConsent {
		private static final String YES = "Yes";

		private String protocol;

		private String regId;

		private Boolean consented;

		private Map<String, String> responses = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		public String getProtocol() {
			return protocol;
		}

		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}

		public String getRegId() {
			return regId;
		}

		public void setRegId(String regId) {
			this.regId = regId;
		}

		public Boolean getConsented() {
			return consented;
		}

		public void setConsented(Boolean consented) {
			this.consented = consented;
		}

		public Map<String, String> getResponses() {
			return responses;
		}

		public void setResponses(Map<String, String> responses) {
			this.responses = responses;
		}

		public void addResponse(String question, String response) {
			responses.put(question, response);
		}

		public boolean hasQuestions() {
			return PROTOCOLS_WITH_Q.indexOf(getProtocol()) != -1;
		}

		public boolean isConsentedToAny(Collection<String> questions) {
			if (!hasQuestions()) {
				return Boolean.TRUE.equals(getConsented());
			}

			if (responses == null || responses.isEmpty()) {
				return false;
			}

			for (String question : questions) {
				String response = responses.get(question);
				if (StringUtils.equalsIgnoreCase(response, YES)) {
					return true;
				}
			}

			return false;
		}
	}
}
