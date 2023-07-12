package com.krishagni.openspecimen.medirex;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationListener;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.TransactionalThreadLocals;
import com.krishagni.catissueplus.core.de.domain.DeObject;

public class SpecimenSaveHandler implements ApplicationListener<SpecimenSavedEvent> {

	private static final String RECV_DATE_FMT = "yyMMdd";

	private DaoFactory daoFactory;

	private ThreadLocal<Map<Long, Specimen>> primarySpecimens = new ThreadLocal<>() {
		protected Map<Long, Specimen> initialValue() {
			TransactionalThreadLocals.getInstance().register(this);
			return new HashMap<>();
		}
	};

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public void onApplicationEvent(SpecimenSavedEvent event) {
		// specimen that is updated in the current transaction
		Specimen updatedSpecimen = event.getEventData();
		if (updatedSpecimen.isDeleted()) {
			return;
		}

		// get hold of the primary specimen
		Specimen primarySpecimen = updatedSpecimen.isPrimary() ? updatedSpecimen : getPrimarySpecimen(updatedSpecimen);

		DeObject customFields = primarySpecimen.getExtension();
		if (customFields == null) {
			return;
		}

		String daycode = (String) getAttrValue(customFields, "day_code");
		if (updatedSpecimen.isPrimary() && updatedSpecimen.isReceived() && StringUtils.isBlank(daycode) && updatedSpecimen.getReceivedEvent().getTime() != null) {
			SimpleDateFormat sdf = new SimpleDateFormat(RECV_DATE_FMT);
			String receivedDate = sdf.format(updatedSpecimen.getReceivedEvent().getTime());
			Long seq = daoFactory.getUniqueIdGenerator().getUniqueId("MEDIREX_DAYCODE", receivedDate);
			daycode = String.format("%s%03d", receivedDate, seq);
			setAttrValue(customFields, "day_code", daycode);
			customFields.saveOrUpdate();
		}

		List<Specimen> workingList = new ArrayList<>();
		if (updatedSpecimen.isPrimary()) {
			workingList.addAll(updatedSpecimen.getDescendants());
		} else {
			workingList.add(updatedSpecimen);
		}

		for (Specimen specimen : workingList) {
			customFields = specimen.getExtension();
			setAttrValue(customFields, "day_code", daycode);
			customFields.saveOrUpdate();
		}
	}

	private Specimen getPrimarySpecimen(Specimen childSpecimen) {
		Long primarySpecimenId = daoFactory.getSpecimenDao().getPrimarySpecimen(childSpecimen.getId());
		Specimen primarySpecimen = primarySpecimens.get().get(primarySpecimenId);
		if (primarySpecimen == null) {
			primarySpecimen = daoFactory.getSpecimenDao().getById(primarySpecimenId);
			primarySpecimens.get().put(primarySpecimenId, primarySpecimen);
		}

		return primarySpecimen;
	}

	private Object getAttrValue(DeObject customFields, String name) {
		DeObject.Attr attr = getAttr(customFields, name);
		return attr != null ? attr.getValue() : null;
	}

	private DeObject.Attr getAttr(DeObject customFields, String name) {
		if (customFields == null || customFields.getAttrs() == null || StringUtils.isBlank(name)) {
			return null;
		}

		DeObject.Attr resultAttr = null;
		for (DeObject.Attr attr : customFields.getAttrs()) {
			if (attr.getName().equals(name) || attr.getUdn().equals(name)) {
				resultAttr = attr;
				break;
			}
		}

		return resultAttr;
	}

	private void setAttrValue(DeObject customFields, String name, Object value) {
		DeObject.Attr resultAttr = getAttr(customFields, name);
		if (resultAttr == null) {
			resultAttr = new DeObject.Attr();
			resultAttr.setName(name);
			resultAttr.setUdn(name);
			customFields.getAttrs().add(resultAttr);
		}

		resultAttr.setValue(value);
	}
}
