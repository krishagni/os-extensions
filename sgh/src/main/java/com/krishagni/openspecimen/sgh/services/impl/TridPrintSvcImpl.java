package com.krishagni.openspecimen.sgh.services.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;

import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.DaoFactoryImpl;
import com.krishagni.catissueplus.core.biospecimen.services.impl.DefaultSpecimenLabelPrinter;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJob;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.openspecimen.sgh.events.TridsRePrintOpDetail;
import com.krishagni.openspecimen.sgh.SghErrorCode;
import com.krishagni.openspecimen.sgh.events.BulkTridPrintOpDetail;
import com.krishagni.openspecimen.sgh.services.TridGenerator;
import com.krishagni.openspecimen.sgh.services.TridPrintSvc;

public class TridPrintSvcImpl implements TridPrintSvc {

	private static final String SGH_MODULE = "plugin_sgh";
	
	private ConfigurationService cfgSvc;
	
	private TridGenerator tridGenerator;
	
	private DaoFactory daoFactory;
	
	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}
	
	public void setTridGenerator(TridGenerator tridGenerator) {
		this.tridGenerator = tridGenerator;
	}
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> generateAndPrintTrids(RequestEvent<BulkTridPrintOpDetail> req) {
		BulkTridPrintOpDetail printReq = req.getPayload();
		int tridsCount = printReq.getTridCount();
		if (tridsCount < 1) {
			return ResponseEvent.userError(SghErrorCode.INVALID_TRID_COUNT);
		}
		
		DefaultSpecimenLabelPrinter printer = getLabelPrinter();
		if (printer == null) {
			throw OpenSpecimenException.serverError(SpecimenErrorCode.NO_PRINTER_CONFIGURED);
		}
		
		List<Specimen> specimens = new ArrayList<Specimen>();
		for(int i = 0; i < printReq.getTridCount(); i++){
			String trid = tridGenerator.getNextTrid();
			specimens.addAll(getSpecimensToPrint(trid));
		}
		
		int copiesToPrint = cfgSvc.getIntSetting(SGH_MODULE, "copies_to_print", 1);
		
		LabelPrintJob job = printer.print(specimens, copiesToPrint);
		if (job == null) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.PRINT_ERROR);
		}
		return ResponseEvent.response(true);
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> printTrids(RequestEvent<TridsRePrintOpDetail> req){
		List<String> tridsToPrint = req.getPayload().getTrids();
		
		List<String> plannedTrids = getPlannedTrids(tridsToPrint);
		if(CollectionUtils.isNotEmpty(plannedTrids)){
			throw OpenSpecimenException.userError(SghErrorCode.CANNOT_PRINT_PLANNED_TRID, StringUtils.join(plannedTrids, ","));
		}
		
//		List<String> unplannedTrides = getUnplannedTrids(tridsToPrint);
//		
//		List<String> invalidTrids = CollectionUtils.subtract(tridsToPrint, unplannedTrides);
//		
		List<String> validTrids = getUnplannedTrids(tridsToPrint);
//	
		List<String> invalidTrids = tridsToPrint;
		invalidTrids.removeAll(validTrids);
		if(CollectionUtils.isNotEmpty(invalidTrids)){
			throw OpenSpecimenException.userError(SghErrorCode.INVALID_TRID_SPECIFIED, StringUtils.join(invalidTrids, ","));
		}
		
		DefaultSpecimenLabelPrinter printer = getLabelPrinter();
		if (printer == null) {
			throw OpenSpecimenException.serverError(SpecimenErrorCode.NO_PRINTER_CONFIGURED);
		}
		
		List<Specimen> specimens = new ArrayList<Specimen>();
		for(String trid : validTrids){
			specimens.addAll(getSpecimensToPrint(trid));
		}
		
		int copiesToPrint = cfgSvc.getIntSetting(SGH_MODULE, "copies_to_print", 1);
		
		LabelPrintJob job = printer.print(specimens, copiesToPrint);
		if (job == null) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.PRINT_ERROR);
		}
		
		return ResponseEvent.response(true);
	}
	
	private Collection<Specimen> getSpecimensToPrint(String visitName) {
		
		List<Specimen> specimens = new ArrayList<Specimen>();
		Visit visit = new Visit();
		visit.setName(visitName);
		
		int tridCopies = cfgSvc.getIntSetting(SGH_MODULE, "trid_copies", 2);
		
		for(int i = 0; i < tridCopies; ++i){
			Specimen specimen = new Specimen();
			specimen.setVisit(visit);
			specimen.setLabel(visitName);
			specimens.add(specimen);
		}
		
		String malignantAliqPrefix = visitName + "_" + getMalignantAliqSuffix() + "_"; 
		for (int i = 1; i <= getMalignantAliqCnt(); ++i) {
			Specimen aliquot = new Specimen();
			aliquot.setVisit(visit);
			aliquot.setLabel(malignantAliqPrefix + i);
			specimens.add(aliquot);
		}
		
		String nonMalignantAliqPrefix = visitName + "_" + getNonMalignantAliqSuffix() + "_";
		for (int i = 1; i <= getNonMalignantAliqCnt(); ++i) {
			Specimen aliquot = new Specimen();
			aliquot.setVisit(visit);
			aliquot.setLabel(nonMalignantAliqPrefix + i);
			specimens.add(aliquot);
		}
		return specimens;
	}
	
	private List<String> getUnplannedTrids(List<String> tridsToPrint) {
		Query query = ((DaoFactoryImpl)daoFactory).getSessionFactory()
				.getCurrentSession()
				.createSQLQuery(GET_UNPLANNED_TRIDS);
		
		query.setParameterList("trids", tridsToPrint);
		return query.list();
	}

	private List<String> getPlannedTrids(List<String> tridsToPrint) {
		Query query = ((DaoFactoryImpl)daoFactory).getSessionFactory()
				.getCurrentSession()
				.createSQLQuery(GET_PLANNED_TRIDS);
		query.setParameterList("trids", tridsToPrint);
		return query.list();
	}
	
	private DefaultSpecimenLabelPrinter getLabelPrinter() {
		String labelPrinterBean = cfgSvc.getStrSetting(
				ConfigParams.MODULE, 
				ConfigParams.SPECIMEN_LABEL_PRINTER, 
				"defaultTridPrinter");
		return (DefaultSpecimenLabelPrinter)OpenSpecimenAppCtxProvider
				.getAppCtx()
				.getBean(labelPrinterBean);
	}

	private int getMalignantAliqCnt() {
		return cfgSvc.getIntSetting(SGH_MODULE, "malignant_aliq_cnt", 3);
	}
	
	private int getNonMalignantAliqCnt() {
		return cfgSvc.getIntSetting(SGH_MODULE, "non_malignant_aliq_cnt", 2);
	}

	private String getMalignantAliqSuffix() {
		return cfgSvc.getStrSetting(SGH_MODULE, "malignant_aliq_suffix", "FZ-T");
	}
	
	private String getNonMalignantAliqSuffix() {
		return cfgSvc.getStrSetting(SGH_MODULE, "non_malignant_aliq_suffix", "FZ2-N");
	}
	
	private static final String GET_PLANNED_TRIDS = 
			"select " +
			"  protocol_participant_id " + 
			"from " +
			"  catissue_coll_prot_reg cpr " +
			"where " +
			"  cpr.protocol_participant_id in (:trids)";
	
	private static final String GET_UNPLANNED_TRIDS = 
			"select " +
			"  distinct item_label " +
			"from " +
			"  os_label_print_job_items jobItems " +
			"where " +
			" item_label in (:trids)";
	
}
