package com.krishagni.openspecimen.usb.token;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.openspecimen.usb.error.PrintTokenErrorCode;

public class SpecimenLISDayNumberPrintToken extends AbstractLabelTmplToken implements LabelTmplToken {
	private final static String LIS_DAY_NUMBER_UDN_NAME = "usb_lis_day_number_3";
	
	@Override
	public String getName() {
		return "usb_specimen_lis_day_number";
	}

	@Override
	public String getReplacement(Object object) {
		Specimen spmn = (Specimen) object;
		
		String lisDayNumber = getLisDayNumberValue(spmn);
		String recievedDateYear = toYear(spmn.getReceivedEvent().getTime());
		
		return lisDayNumber.isEmpty() ? StringUtils.EMPTY : recievedDateYear + "-" + lisDayNumber;
	}
	
	public String getLisDayNumberValue(Specimen spmn) {
		String attrName = getLisDayNumberAttrName(spmn);
		
		for (Entry<String, Object> entry : spmn.getExtension().getAttrValues().entrySet()) {
			if (entry.getKey() != null && entry.getKey().equals(attrName)) {
				String lisDayNumber = (String) entry.getValue();
				return lisDayNumber != null ? lisDayNumber : StringUtils.EMPTY;
			}
		}
		
		return StringUtils.EMPTY;
	}

	private String getLisDayNumberAttrName(Specimen spmn) {
		return getAttrName(spmn, LIS_DAY_NUMBER_UDN_NAME)
				.orElseThrow(() -> 
				OpenSpecimenException.userError(PrintTokenErrorCode.INVALID_LIS_DAY_NUMBER_UDN, LIS_DAY_NUMBER_UDN_NAME)
						);
	}

	private Optional<String> getAttrName(Specimen spmn, String attrUdn) {
		return spmn.getExtension().getAttrs()
				.stream()
				.filter(k -> k.getUdn().equals(attrUdn))
				.map(DeObject.Attr::getName)
				.findFirst();
	}
	
	private String toYear(Date date) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy");
		return formatter.format(date);
	}
}
