package com.krishagni.openspecimen.usb.token;

import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.domain.AbstractLabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.openspecimen.usb.error.PrintTokenErrorCode;

public class SpecimenLISGermCodePrintToken extends AbstractLabelTmplToken implements LabelTmplToken {
	private final static String LIS_GERM_CODE_UDN_NAME = "usb_lis_germ_code_1";

	public SpecimenLISGermCodePrintToken() {

	}

	@Override
	public String getName() {
		return "usb_specimen_lis_germ_code";
	}

	@Override
	public String getReplacement(Object object) {
		Specimen spmn = (Specimen) object;
		return getGermAbbrValue(spmn);
	}

	private String getGermAbbrValue(Specimen spmn) {
		String attrName = getGermAbbrAttrName(spmn);

		for (Entry<String, Object> entry : spmn.getExtension().getAttrValues().entrySet()) {
			if (entry.getKey() != null && entry.getKey().equals(attrName)) {
				return (String) entry.getValue();
			}
		}

		return StringUtils.EMPTY;
	}

	private String getGermAbbrAttrName(Specimen spmn) {
		return getAttrName(spmn, LIS_GERM_CODE_UDN_NAME)
				.orElseThrow(() -> 
				OpenSpecimenException.userError(PrintTokenErrorCode.INVALID_LIS_GERM_CODE_UDN, LIS_GERM_CODE_UDN_NAME)
						);
	}

	private Optional<String> getAttrName(Specimen spmn, String attrUdn) {
		return spmn.getExtension().getAttrs()
				.stream()
				.filter(k -> k.getUdn().equals(attrUdn))
				.map(DeObject.Attr::getName)
				.findFirst();
	}
}
