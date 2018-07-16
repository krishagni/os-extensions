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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Patient {
	private static final Log logger = LogFactory.getLog(Patient.class);

	//
	// TODO: pick from config
	//
	private static final List<String> PROTOCOLS_WITH_Q = Arrays.asList("06-107", "SPC-POE");

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

	public void addProtocolConsent(String protocol, String regId, Boolean consented, String question, String answer) {
		ProtocolConsent pc = protocolConsents.get(protocol);
		if (pc == null) {
			pc = new ProtocolConsent();
			pc.setProtocol(protocol);
			protocolConsents.put(protocol, pc);
		}

		pc.setRegId(regId);
		pc.setConsented(consented);
		pc.addResponse(question, answer);
	}


	public boolean isConsented(Date visitDate, Map<String, Collection<String>> protocolQuestions) {
		boolean consented = false;

		if (!isAlive() && visitDate.before(CONSENT_WAIVED_UNTIL)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Patient (" + getMrn() + ") is not alive. Visit date is before " + CONSENT_WAIVED_UNTIL);
			}

			consented = true;
		} else if (isAlive()) {
			for (Map.Entry<String, Collection<String>> pq : protocolQuestions.entrySet()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Checking whether patient (" + getMrn() + ") has consented to the protocol: " + pq.getKey());
				}

				ProtocolConsent pc = protocolConsents.get(pq.getKey());
				if (pc == null) {
					if (logger.isDebugEnabled()) {
						logger.debug("Patient (" + getMrn() + ") consent info not available for the protocol: " + pq.getKey());
					}

					continue;
				}

				if (pc.hasConsentedTo(pq.getValue())) {
					if (logger.isDebugEnabled()) {
						logger.debug("Patient (" + getMrn() + ") has consented to the protocol: " + pq.getKey());
					}

					consented = true;
					break;
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Patient (" + getMrn() + ") has not consented to the protocol: " + pq.getKey());
				}
			}

			//
			// No consents configured... therefore assume the patient has consented.
			//
			if (!consented && protocolQuestions.isEmpty()) {
				consented = true;
			}
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Patient (" + getMrn() + ") is dead. Visit date is after " + CONSENT_WAIVED_UNTIL);
			}
		}

		return consented;
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
			if (StringUtils.isBlank(question)) {
				return;
			}

			responses.put(question, response);
		}

		public boolean hasQuestions() {
			return PROTOCOLS_WITH_Q.indexOf(getProtocol()) != -1;
		}

		public boolean hasConsentedTo(Collection<String> questions) {
			if (!hasQuestions()) {
				return Boolean.TRUE.equals(getConsented());
			}

			if (responses == null || responses.isEmpty()) {
				return false;
			}

			return questions.stream().allMatch(q -> StringUtils.equalsIgnoreCase(responses.get(q), YES));
		}
	}
}
