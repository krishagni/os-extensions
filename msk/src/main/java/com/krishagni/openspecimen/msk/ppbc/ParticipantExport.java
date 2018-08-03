package com.krishagni.openspecimen.msk.ppbc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseExtensionEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.repository.CprListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;

@Configurable
public class ParticipantExport implements ScheduledTask {
    private static final Log logger = LogFactory.getLog(ParticipantExport.class);

    @Autowired
    private DaoFactory daoFactory;
   
    @Override
    public void doJob(ScheduledJobRun jobRun) {
        exportParticipants();
    }

    private void exportParticipants() {
        CsvFileWriter csvFileWriter = null;
        SpecimenExport specimenExport = new SpecimenExport();
        try {
        	
            csvFileWriter = getCSVWriter();
            csvFileWriter.writeNext(getHeader());

            boolean endOfParticipants = false;
            Long lastId = 0L;
            int maxRecs = 100;

            while (!endOfParticipants) {
                List<CollectionProtocolRegistration> cprs = exportParticipants(csvFileWriter, specimenExport, lastId, maxRecs);
                
                if (!cprs.isEmpty()) {
                	lastId = cprs.get(cprs.size()-1).getId();
                }

                endOfParticipants = (cprs.size() < maxRecs);
            }        
        } catch (Exception e) {
            logger.error("Error while running participant export job", e);
        } finally {
            IOUtils.closeQuietly(csvFileWriter);
            IOUtils.closeQuietly(specimenExport.getCsvFileWriter());
        }
    }
    
    private Map<String, String> getCustomFieldValueMap(BaseExtensionEntity obj) {
    	return obj.getExtension().getAttrs().stream().collect(
    			Collectors.toMap(
    				attr -> attr.getCaption(),
    				attr -> attr.getDisplayValue(""),
    				(v1, v2) -> {throw new IllegalStateException("Duplicate key");},
    				LinkedHashMap::new)
    			);
    }
    
    private CsvFileWriter getCSVWriter() {
    	String exportFolder = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        File file = new File(ConfigUtil.getInstance().getDataDir() + File.separatorChar + exportFolder, "Accession" + ".csv");
        return CsvFileWriter.createCsvFileWriter(file);
    }
    
    private static List<String> splitToMultiple(String inputString, int maxSize, String delimiter) {
    	if (inputString == null) {
            inputString = "";
    	}
    	
    	String[] maxSizeBufferArray = new String[maxSize];
    	String[] splittedArray = inputString.split(delimiter);
    	List<String> multipleAttrs = new ArrayList<>();
    	int i = 0;
	    
	while (i < maxSize) {
	    if (i < splittedArray.length) {
	    	maxSizeBufferArray[i] = splittedArray[i];
	    } else {
	    	maxSizeBufferArray[i] = "";
	    }
	    i++;
	}
	    
    	for (String site : maxSizeBufferArray) {
    		multipleAttrs.add(site);
    	}
    	
    	return multipleAttrs;
    }
    
    ///////////////////
    //
    // Collection Protocol Registrations
    //
    ///////////////////

    @PlusTransactional
    private List<CollectionProtocolRegistration> exportParticipants(CsvFileWriter csvFileWriter, SpecimenExport specimenExport, Long lastId, int maxRecs) throws IOException {
    	CprListCriteria cprListCriteria = new CprListCriteria().lastId(lastId).maxResults(maxRecs);
    	List<CollectionProtocolRegistration> cprs = daoFactory.getCprDao().getCprs(cprListCriteria);
        
        cprs.forEach(cpr -> processCpr(cpr, csvFileWriter, specimenExport));
        
        csvFileWriter.flush();
        return cprs;
    }
    
    ///////////////////
    //
    // Participants
    //
    ///////////////////

    private void processCpr(CollectionProtocolRegistration cpr, CsvFileWriter csvFileWriter, SpecimenExport specimenExport) {
    	List<String> props = new ArrayList<>();
    	
    	props.add(cpr.getParticipant().getEmpi());
    	props.add((String) (getCustomFieldValueMap(cpr.getParticipant()).getOrDefault("Darwin ID", "")));
    	
    	if (!cpr.getVisits().isEmpty()) {
            cpr.getVisits().forEach(visit -> processVisit(visit, props, csvFileWriter, specimenExport));
    	} else {
            csvFileWriter.writeNext(props.toArray(new String[props.size()]));
    	}
    }
    
    ///////////////////
    //
    // Visits
    //
    ///////////////////

    private void processVisit(Visit visit, List<String> visitProps, CsvFileWriter csvFileWriter, SpecimenExport specimenExport) {
    	ArrayList<String> props = populateVisit(visit, visitProps);
	
    	if (!visit.getTopLevelSpecimens().isEmpty()) {
    	    Set<Specimen> specimenList = visit.getTopLevelSpecimens();
    	    handleSpecimens(specimenList, props, csvFileWriter, specimenExport);
    	} else {
       	    csvFileWriter.writeNext(props.toArray(new String[props.size()]));
    	}
    }
    
    private void handleSpecimens(Set<Specimen> specimenList, List<String> specimenProps, CsvFileWriter csvFileWriter, SpecimenExport specimenExport) {
    	
    	for (Specimen specimen : specimenList) {
	    	if (specimen.isPrimary()) {
	    		ArrayList<String> props = new ArrayList<>(specimenProps);
	    		processSpecimen(specimen, props, csvFileWriter);
	    	} else if (specimen.isAliquot()) {
	    		specimenExport.exportSpecimens(specimen);
	    	}
	    	
	    	if (specimen.getChildCollection().isEmpty()) {
	    		continue;
	    	} else {
	    		handleSpecimens(specimen.getChildCollection(), specimenProps, csvFileWriter, specimenExport);
	    	}
    	}
    }
    
    private ArrayList<String> populateVisit(Visit visit, List<String> visitProps) {
    	ArrayList<String> props = new ArrayList<String>(visitProps);
		    	
    	props.add(visit.getName());
    	props.add(visit.getVisitDate().toString()); 
    	props.add(getSiteName(visit));
    	props.add(getClinicalDiagnoses(visit));
    	props.add(visit.getSurgicalPathologyNumber()); 
    	props.add(visit.getComments());
    	props.addAll(getCustomField(visit));
		
    	return props;
    }
    
    private String getSiteName(Visit visit) {
    	return visit.getSite() != null ? visit.getSite().getName() : "";
    }
    
    private String getClinicalDiagnoses(Visit visit) {
    	return visit.getClinicalDiagnoses().isEmpty() ? "" : visit.getClinicalDiagnoses().iterator().next();
    }
    
    private List<String> getCustomField(Visit visit) {
    	ArrayList<String> row = new ArrayList<String>();
    	Map<String, String> customFieldValueMap = getCustomFieldValueMap(visit);
    	
    	row.add((String) (customFieldValueMap.getOrDefault("Diagnosis Notes","")));
    	row.add((String) (customFieldValueMap.getOrDefault("Operation Date","")));
    	row.add((String) (customFieldValueMap.getOrDefault("Path Date","")));
    	row.add((String) (customFieldValueMap.getOrDefault("Surgeon Name","")));
    	row.add((String) (customFieldValueMap.getOrDefault("Specimen Description","")));
    	row.add((String) (customFieldValueMap.getOrDefault("NUN (N)","")));
    	row.add((String) (customFieldValueMap.getOrDefault("NUN (T)","")));
    	row.add((String) (customFieldValueMap.getOrDefault("OCT (N)","")));
    	row.add((String) (customFieldValueMap.getOrDefault("OCT (T)","")));
    	
    	return row;
    }
    
    ///////////////////
    //
    // Specimen
    //
    ///////////////////
	
    private void processSpecimen(Specimen specimen, List<String> specimenProps, CsvFileWriter csvFileWriter) {
    	ArrayList<String> props = populateSpecimen(specimen, specimenProps);

    	csvFileWriter.writeNext(props.toArray(new String[props.size()]));
    }

    private ArrayList<String> populateSpecimen(Specimen specimen, List<String> specimenProps) {
    	ArrayList<String> props = new ArrayList<String>(specimenProps);
    	
    	props.add(specimen.getLabel());
    	props.add(specimen.getSpecimenType());
    	props.addAll(getMultipleAnatomicSites(specimen));
    	props.add(specimen.getTissueSide());
    	props.add(specimen.getPathologicalStatus());
    	props.add(specimen.getComment());
    	props.add(specimen.getCollRecvDetails().getCollTime().toString());
    	props.add(specimen.getCollRecvDetails().getRecvTime().toString());
    	props.addAll(getCustomField(specimen));

    	return props;
    }
    
    private List<String> getMultipleAnatomicSites(Specimen specimen) {
    	return splitToMultiple(specimen.getTissueSite(), 3, "/");
    }
    
    private List<String> getCustomField(Specimen specimen) {
    	ArrayList<String> row = new ArrayList<String>();
    	Map<String, String> customFieldValueMap = getCustomFieldValueMap(specimen);
    	
    	row.add((String) (customFieldValueMap.getOrDefault("Part Number","")));
    	row.add((String) (customFieldValueMap.getOrDefault("Part Sub Number","")));
    	row.add((String) (customFieldValueMap.getOrDefault("Biobank Technician","")));
    	row.add((String) (customFieldValueMap.getOrDefault("Accessioning Temperature Condition","")));
    	row.add((String) (customFieldValueMap.getOrDefault("Biobank Temperature","")));
    	row.add((String) (customFieldValueMap.getOrDefault("Location","")));
    	row.add((String) (customFieldValueMap.getOrDefault("Accessioned Time","")));
    	row.addAll(splitToMultiple(customFieldValueMap.getOrDefault("Histology Data",""), 4, "/"));
    	row.add((String) (customFieldValueMap.getOrDefault("Harvestor","")));
    	
    	return row;
    }
    
    private String[] getHeader() {
        return new String[] {
                // Participant Headers
        	"TBA_CRDB_MRN",
                "TBA_PT_DEIDENTIFICATION_ID",
                
                // Visit Headers
                "TBD_BANK_NUM",
                "TBA_PROCUREMENT_DTE",
                "TBD_BANK_SUB_CD",
                "TBA_DISEASE_DESC",
                "TBA_ACCESSION_NUM",
                "TBD_BANK_NOTE",
                "TBA_DIAGNOSIS_NOTE",
                "TBA_SURG_STRT_DT",
                "TBA_PATH_REVIEW_DT",
                "TBA_SURGEON_NAME",
                "SURGICAL_PATH_REPORT",
                "TBD_NUN_N",	
                "TBD_NUN_T",
                "TBD_OCT_N",
                "TBD_OCT_T",
                
                // Specimen Headers
                "PARENT_SPECIMEN_LABEL",
                "TBD_SPECIMEN_TYPE_DESC",   
                "TBA_SITE_DESC",
                "TBA_SUB_SITE_DESC",
                "TBA_SUB2_SITE_DESC",
                "TBA_SITE_SIDE_DESC",
                "TBA_TISSUE_TYPE_DESC",        
                "TBA_SITE_TEXT",        
                "TBA_RESECT_DT",      
                "TBA_BIOBANK_RECEIPT_DT",
                "TBA_PART_NUM",
                "TBA_SUB_PART_NUM",
                "TBA_BIOBANK_TECH_NAME",
                "TBA_TEMPERATURE_COND_DESC",
                "TBA_BIOBANK_TEMPERATURE_COND_DESC",
                "TBA_SITE_LOCATION_DESC",
                "TBA_ACCESSION_RECEIPT_DT",
                "TBA_HISTOLOGY_DESC",
                "TBA_HISTOLOGY_SUB_DESC", 
                "TBA_HISTOLOGY_SUB2_DESC", 
                "TBA_HISTOLOGY_SUB3_DESC", 
                "TBA_HARVEST_PA_NAME"
        };
    }
}
