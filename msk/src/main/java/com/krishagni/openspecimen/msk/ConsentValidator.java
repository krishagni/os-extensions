package com.krishagni.openspecimen.msk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.DpConsentTier;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionOrderErrorCode;
import com.krishagni.catissueplus.core.administrative.services.DistributionValidator;
import com.krishagni.catissueplus.core.biospecimen.domain.ConsentStatement;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Utility;

public class ConsentValidator implements DistributionValidator {

	private static final Log logger = LogFactory.getLog(ConsentValidator.class);

	@Override
	public String getName() {
		return "consents";
	}

	@Override
	public void validate(DistributionProtocol dp, List<Specimen> specimens, Map<String, Object> ctxt) {
		List<String[]> questions = Utility.nullSafeStream(dp.getConsentTiers())
			.map(DpConsentTier::getStatement)
			.map(ConsentStatement::getStatement)
			.map(stmt -> stmt.split(":", 2))
			.collect(Collectors.toList());

		Map<String, Collection<String>> pqMap = new HashMap<>();
		for (String[] question : questions) {
			Collection<String> pqs = pqMap.computeIfAbsent(question[0].trim(), this::newList);
			if (question.length > 1) {
				pqs.add(question[1].trim());
			}
		}

		PatientDb db = null;
		try {
			db = new PatientDb();

			Map<ErrorCode, List<String>> errorsMap = new HashMap<>();
			Map<String, Patient> patients = new HashMap<>();
			for (Specimen specimen : specimens) {
				String mrn = specimen.getRegistration().getParticipant().getEmpi();
				if (StringUtils.isBlank(mrn)) {
					List<String> noMrnLabels = errorsMap.computeIfAbsent(MskError.NO_MRN, this::newList);
					noMrnLabels.add(specimen.getLabel());
					continue;
				}

				Date visitDate = specimen.getVisit().getVisitDate();
				Patient patient = null;
				if (!patients.containsKey(mrn)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Retrieving consent info of patient: " + mrn);
					}

					patient = db.getByMrn(mrn);
					patients.put(mrn, patient);

					if (patient == null && logger.isDebugEnabled()) {
						logger.debug("No patient: " + mrn);
					}
				}

				patient = patients.get(mrn);
				if (patient == null) {
					List<String> noPatientLabels = errorsMap.computeIfAbsent(MskError.PATIENT_NOT_FOUND, this::newList);
					noPatientLabels.add(specimen.getLabel());
					continue;
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Checking consent status of patient: " + mrn);
				}

				if (!patient.isConsented(visitDate, pqMap)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Patient (" + mrn + ") has not consented to any protocols: " + pqMap.keySet());
					}

					List<String> nonConsenting = errorsMap.computeIfAbsent(DistributionOrderErrorCode.NON_CONSENTING_SPECIMENS, this::newList);
					nonConsenting.add(specimen.getLabel());
					continue;
				}
			}

			if (!errorsMap.isEmpty()) {
				OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
				errorsMap.forEach(ose::addError);
				throw ose;
			}
		} finally {
			IOUtils.closeQuietly(db);
		}
	}

	private <K, V> List<V> newList(K k) {
		return new ArrayList<>();
	}
}
