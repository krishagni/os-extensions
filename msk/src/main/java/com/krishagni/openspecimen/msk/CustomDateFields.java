package com.krishagni.openspecimen.msk;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseExtensionEntity;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;

public class CustomDateFields {
	private static Log logger = LogFactory.getLog(CustomDateFields.class);

	private static final String DATE_FIELD_NAMES = "msk/date_fields.properties";

	private static CustomDateFields instance = new CustomDateFields();

	private String pathDate        = "pathDate";

	private String operationDate   = "operationDate";

	private String accessionedDate = "accessionDate";

	private CustomDateFields() {
		File file = new File(DATE_FIELD_NAMES);
		if (!file.exists()) {
			return;
		}

		try (FileInputStream fin = new FileInputStream(file)) {
			Properties props = new Properties();
			props.load(fin);

			pathDate        = props.getProperty("visit.path_date", "pathDate");
			operationDate   = props.getProperty("visit.operation_date", "operationDate");
			accessionedDate = props.getProperty("specimen.accessioned_date", "accessionDate");
		} catch (Exception e) {
			logger.fatal("Error loading visit and specimen custom date field names", e);
		}
	}

	public static CustomDateFields getInstance() {
		return instance;
	}

	public Date getPathDate(BaseExtensionEntity obj) {
		return Utility.chopTime(getDate(obj, pathDate));
	}

	public Date getOperationDate(BaseExtensionEntity obj) {
		return Utility.chopTime(getDate(obj, operationDate));
	}

	public Date getAccessionedDate(BaseExtensionEntity obj) {
		return getDate(obj, accessionedDate);
	}

	private Date getDate(BaseExtensionEntity obj, String udn) {
		if (obj == null || obj.getExtension() == null) {
			return null;
		}

		DeObject.Attr attr = Utility.nullSafeStream(obj.getExtension().getAttrs())
			.filter(a -> a.getUdn().equals(udn))
			.findFirst().orElse(null);

		if (attr == null || attr.getValue() == null) {
			return null;
		} else if (attr.getValue() instanceof Date) {
			return (Date) attr.getValue();
		} else {
			return new Date(Long.parseLong((String)attr.getValue()));
		}
	}
}
