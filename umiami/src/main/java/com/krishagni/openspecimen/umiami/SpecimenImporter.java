package com.krishagni.openspecimen.umiami;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.common.domain.ExternalAppId;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;

public class SpecimenImporter implements ObjectImporter<SampleDetail, Boolean> {
	private static final Log logger = LogFactory.getLog(SpecimenImporter.class);

	private static final String EXT_APP_NAME = "Nautilus";

	private DaoFactory daoFactory;

	private SpecimenService specimenSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setSpecimenSvc(SpecimenService specimenSvc) {
		this.specimenSvc = specimenSvc;
	}
	
	@Override
	public ResponseEvent<Boolean> importObject(RequestEvent<ImportObjectDetail<SampleDetail>> req) {
		ImportObjectDetail<SampleDetail> importDetail = req.getPayload();
		SampleDetail parentSample = importDetail.getObject();
		
		try {
			Specimen parentSpmn = daoFactory.getSpecimenDao().getByLabel(parentSample.getParent());
			if (parentSpmn == null) {
				return ResponseEvent.userError(SpecimenErrorCode.NOT_FOUND, parentSample.getParent());
			}

			Map<String, List<SpecimenDetail>> aliquotsByType = new HashMap<>();
			Map<String, String> aliquotsDigest               = new HashMap<>();

			for (SampleDetail inputAliquot : parentSample.getChildren()) {
				ExternalAppId extAppId = getExtAppId(inputAliquot.getName());
				if (extAppId == null) {
					addAliquot(aliquotsByType, parentSpmn, inputAliquot);
					aliquotsDigest.put(inputAliquot.getName(), inputAliquot.getHashDigest());
				} else {
					updateAliquot(inputAliquot, extAppId);
				}
			}

			saveAliquots(aliquotsByType, aliquotsDigest);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
		
		return ResponseEvent.response(true);
	}

	private void addAliquot(Map<String, List<SpecimenDetail>> aliquotsByType, Specimen parentSpmn, SampleDetail inputAliquot) {
		String parentType  = parentSpmn.getSpecimenType();
		String aliquotType = inputAliquot.getMatrixType();

		if (parentType.equals(aliquotType)) {
			List<SpecimenDetail> aliquots = aliquotsByType.computeIfAbsent(parentType, (k) -> new ArrayList<>());
			aliquots.add(toSpecimen(inputAliquot, null, parentSpmn.getId()));
		} else {
			List<SpecimenDetail> derivatives = aliquotsByType.computeIfAbsent(aliquotType, (k) -> new ArrayList<>());

			SpecimenDetail derivative;
			if (derivatives.isEmpty()) {
				derivative = toDerivative(parentSpmn, aliquotType);
				derivatives.add(derivative);
			} else {
				derivative = derivatives.iterator().next();
			}

			derivative.getChildren().add(toSpecimen(inputAliquot));
		}
	}

	private void updateAliquot(SampleDetail inputAliquot, ExternalAppId extAppId) {
		Specimen existingAliquot = daoFactory.getSpecimenDao().getByLabel(inputAliquot.getName());
		if (existingAliquot == null) {
			logger.info(String.format(
				"Aliquot %s mapped by the external ID %d no longer exists in the DB",
				inputAliquot.getName(), extAppId.getId()));
			return;
		}

		if (existingAliquot.isClosed()) {
			logger.info(String.format("Aliquot %s is closed. No updates will be applied", inputAliquot.getName()));
			return;
		}

		if (inputAliquot.getHashDigest().equals(extAppId.getProp("hash"))) {
			logger.debug(String.format(
				"Aliquot %s data items have not changed since last update",
				inputAliquot.getName()));
			return;
		}

		SpecimenDetail toSave = toSpecimen(inputAliquot, existingAliquot.getId(), null);
		response(specimenSvc.updateSpecimen(request(toSave)));
		saveExtId(extAppId, inputAliquot.getHashDigest());
	}

	private void saveAliquots(Map<String, List<SpecimenDetail>> aliquotsByType, Map<String, String> aliquotsDigest) {
		List<SpecimenDetail> toSave = aliquotsByType.values().stream()
			.flatMap(List::stream).collect(Collectors.toList());

		List<SpecimenDetail> savedSpmns = response(specimenSvc.collectSpecimens(request(toSave)));
		for (SpecimenDetail savedAliquot : getSavedAliquots(savedSpmns)) {
			saveExtId(savedAliquot.getLabel(), savedAliquot.getId(), aliquotsDigest.get(savedAliquot.getLabel()));
		}
	}

	private SpecimenDetail toSpecimen(SampleDetail input) {
		return toSpecimen(input, null, null);
	}

	private SpecimenDetail toSpecimen(SampleDetail input, Long id, Long parentId) {
		SpecimenDetail aliquot = new SpecimenDetail();
		if (id != null) {
			aliquot.setId(id);
		}

		if (parentId != null) {
			aliquot.setParentId(parentId);
		}

		aliquot.setLabel(input.getName());
		aliquot.setLineage(Specimen.ALIQUOT);
		aliquot.setType(input.getMatrixType());
		aliquot.setAvailableQty(input.getAmount());
		aliquot.setStatus(Specimen.COLLECTED);
		aliquot.setActivityStatus(isDepleted(input) ?
			Status.ACTIVITY_STATUS_CLOSED.getStatus() : Status.ACTIVITY_STATUS_ACTIVE.getStatus());

		Map<String, Object> attrsMap = new HashMap<>();
		attrsMap.put("avg260280", input.getAvg260280());
		attrsMap.put("concByNano", input.getConcByNano());
		attrsMap.put("concByNanoUnit", input.getConcByNanoUnit());
		attrsMap.put("concByOther", input.getConcByOther());
		attrsMap.put("concByOtherUnit", input.getConcByOtherUnit());
		attrsMap.put("concByQubit", input.getConcByQubit());
		attrsMap.put("concByQubitUnit", input.getConcByQubitUnit());
		attrsMap.put("qubitMethod", input.getQubitMethod());
		attrsMap.put("processingComments", input.getProcessingComments());

		ExtensionDetail customFields = new ExtensionDetail();
		customFields.setAttrsMap(attrsMap);
		aliquot.setExtensionDetail(customFields);

		return aliquot;
	}

	private SpecimenDetail toDerivative(Specimen parent, String type) {
		SpecimenDetail derivative = new SpecimenDetail();
		derivative.setParentId(parent.getId());
		derivative.setLineage(Specimen.DERIVED);
		derivative.setType(type);
		derivative.setStatus(Specimen.COLLECTED);
		derivative.setChildren(new ArrayList<>());
		derivative.setCloseAfterChildrenCreation(true);
		return derivative;
	}

	private boolean isDepleted(SampleDetail aliquot) {
		return aliquot.getDepleted().equalsIgnoreCase("T");
	}

	private List<SpecimenDetail> getSavedAliquots(List<SpecimenDetail> savedSpmns) {
		List<SpecimenDetail> aliquots = new ArrayList<>();
		for (SpecimenDetail savedSpmn : savedSpmns) {
			if (savedSpmn.getLineage().equals(Specimen.DERIVED)) {
				aliquots.addAll(savedSpmn.getChildren());
			} else {
				aliquots.add(savedSpmn);
			}
		}

		return aliquots;
	}

	private ExternalAppId getExtAppId(String extId) {
		return daoFactory.getExternalAppIdDao().getByExternalId(EXT_APP_NAME, Specimen.class.getName(), extId);
	}

	private void saveExtId(String extId, Long osId, String hashDigest) {
		ExternalAppId externalAppId = new ExternalAppId();
		externalAppId.setAppName(EXT_APP_NAME);
		externalAppId.setEntityName(Specimen.class.getName());
		externalAppId.setExternalId(extId);
		externalAppId.setOsId(osId);
		saveExtId(externalAppId, hashDigest);
	}

	private void saveExtId(ExternalAppId extAppId, String digest) {
		extAppId.setProp("hash", digest);
		daoFactory.getExternalAppIdDao().saveOrUpdate(extAppId, true);
	}

	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}