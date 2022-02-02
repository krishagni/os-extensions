package com.krishagni.openspecimen.umiami;

import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.importer.services.ObjectImporterFactory;
import com.krishagni.catissueplus.core.importer.services.ObjectSchemaFactory;

public class PluginInitializer implements InitializingBean {
	private ObjectSchemaFactory objectSchemaFactory;

	private ObjectImporterFactory objectImporterFactory;
	
	private SpecimenImporter specimenImporter;
	
	public void setObjectSchemaFactory(ObjectSchemaFactory objectSchemaFactory) {
		this.objectSchemaFactory = objectSchemaFactory;
	}

	public void setObjectImporterFactory(ObjectImporterFactory objectImporterFactory) {
		this.objectImporterFactory = objectImporterFactory;
	}
	
	public void setSpecimenImporter(SpecimenImporter specimenImporter) {
		this.specimenImporter = specimenImporter;
	}
	
	@Override
	public void afterPropertiesSet() {
		objectSchemaFactory.registerSchema("com/krishagni/openspecimen/umiami/specimen.xml");
		objectImporterFactory.registerImporter("miamiSpecimen", specimenImporter);
	}

}
