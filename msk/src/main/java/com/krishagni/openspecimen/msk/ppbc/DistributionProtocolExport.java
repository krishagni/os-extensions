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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.DistributionOrderItem;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.DpRequirement;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.repository.DpListCriteria;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseExtensionEntity;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.de.domain.DeObject.Attr;

@Configurable
public class DistributionProtocolExport implements ScheduledTask {
	private static final Log logger = LogFactory.getLog(DistributionProtocolExport.class);

	@Autowired
	private DaoFactory daoFactory;
	
	public void doJob(ScheduledJobRun jobRun) {
		export();
	}
	
	private void export() {
		CsvFileWriter dpFileWriter = null, dPRFileWriter = null, doFileWriter = null;
		
		try {
			dpFileWriter = getDpCSVWriter();
			dPRFileWriter = getDpRCSVWriter();
			doFileWriter = getDoCSVWriter();
			
			dpFileWriter.writeNext(getDpHeader());
			dPRFileWriter.writeNext(getDpRHeader());
			doFileWriter.writeNext(getDoHeader());
			
			boolean endOfDPs = false;
			int startAt = 0, maxRecs = 10;
		    
			while (!endOfDPs) {
      			int exportedRecsCount = exportDpData(dpFileWriter, dPRFileWriter, doFileWriter, startAt, maxRecs);
      			startAt += exportedRecsCount;
      			endOfDPs = (exportedRecsCount < maxRecs);
    		}
  		} catch (Exception e) {
  			logger.error("Error while running distribution protocol export job", e);
		} finally {
			IOUtils.closeQuietly(dPRFileWriter);
			IOUtils.closeQuietly(dpFileWriter);
			IOUtils.closeQuietly(doFileWriter);
		}
	}

	@PlusTransactional
	private int exportDpData(CsvFileWriter dpFileWriter, CsvFileWriter dPRFileWriter, CsvFileWriter doFileWriter, int startAt, int maxRecs) throws IOException {
		DpListCriteria listCrit = new DpListCriteria().startAt(startAt).maxResults(maxRecs);
		List<DistributionProtocol> dPs = daoFactory.getDistributionProtocolDao().getDistributionProtocols(listCrit);
		
		exportDp(dpFileWriter, dPs);
		dPs.forEach(dp -> exportDpr(dPRFileWriter, dp.getRequirements()));
		dPs.forEach(dp -> exportDOs(doFileWriter, dp.getDistributionOrders()));
		
		dpFileWriter.flush();
		dPRFileWriter.flush();
		doFileWriter.flush();

		return dPs.size();
	}
	
	private List<String> getCustomFieldValues(BaseExtensionEntity obj) {
		return obj.getExtension()
			.getAttrs().stream()
			.map(Attr::getDisplayValue)
			.collect(Collectors.toList());
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
	
	///////////////////////
	//
	// DP export
	//
	///////////////////////

	private void exportDp(CsvFileWriter dpFileWriter, List<DistributionProtocol> dPs) {
		dPs.forEach(dp -> dpFileWriter.writeNext(getDpRow(dp)));
	}

	private CsvFileWriter getDpCSVWriter() {
		String exportFolder = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
		File outputFile = new File(ConfigUtil.getInstance().getDataDir() + File.separatorChar + exportFolder, "Specimen_Request" + ".csv");
		return CsvFileWriter.createCsvFileWriter(outputFile);
	}

	private String[] getDpHeader() {
		return new String[]{
			"TBR_REQUEST_TITLE",			// Title
			"TBR_SOURCE_REQUEST",			// Shorttitle
			"TBR_INSTITUTE_DESC",			// ReceivingInstitute
			"TBR_DEPT_DESC",			// Department
			"TBR_REQUESTER_DESC",			// PI#Email
			"TBR_REQUEST_DT",			// DPCustomFields#Request
			"TBR_ILAB_NO",				// DateDPCustomFields#ILABNo
			"TBR_FINALIZE_FLAG",			// DPCustomFields#FinalizeFlag
			"TBR_COST_CENTER",			// DPCustomFields#CostCenter
			"TBR_FUND_ID",				// DPCustomFields#FundID
			"TBR_MTA_FLAG",				// DPCustomFields#MTAFlag
			"TBR_DISTRIBUTION_OPTION_DESC",		// DPCustomFields#DistributionOptionDesc
			"TBR_HBC_ID",				// DPCustomFields#HBCID
			"TBR_HBC_COMMITTEE_APPROVAL_DT",	// DPCustomFields#HBCCommitteeApprovalDate
			"TBR_MIN_UNIT",				// DPCustomFields#MinimumUnit
			"TBR_MTA_APPROVAL_DT",			// DPCustomFields#MTAApprovalDate
			"TBR_PICKUP_ARRANGEMENT_DESC",		// DPCustomFields#PickupArrangementDescription
			"TBR_PROSPECT_FLAG",			// DPCustomFields#ProspectFlag
			"TBR_TYPE_DESC",			// DPCustomFields#TypeDescription
			"TBR_RESTROSPECT_FLAG",			// DPCustomFields#RestrospectFlag
			"TBR_SPECIMEN_COLLECTION_METHOD",	// DPCustomFields#SpecimenCollectionMethod
			"TBR_COMMENTS",				// DPCustomFields#Comments
			"TBR_CONTACT_NAME",			// DPCustomFields#ContactName
			"TBR_SPECIMEN_USAGE_DESC",		// DPCustomFields#SpecimenUsage
			"TBR_WAIVER_NO",
			"TBR_MIN_SIZE_DESC",
			// "TBR_DESEASE_DESC",			// Ignored, Already present in Accession sheet
			"TBR_STS_DESC",
			"TBR_SPECIAL_HANDLING_DESC"
		};
	}

	private String[] getDpRow(DistributionProtocol dp) {
		List<String> row = new ArrayList<>();
		Map<String, String> customFieldValueMap = getCustomFieldValueMap(dp);
		
		row.add(dp.getTitle());
		row.add(dp.getShortTitle());
		row.add(dp.getInstitute().getName());
		row.add(getDpReceivingSiteName(dp));
		row.add(dp.getPrincipalInvestigator().getFirstName() + " " + dp.getPrincipalInvestigator().getLastName());
		
		row.add((String) (customFieldValueMap.getOrDefault("Request Date", "")));
		row.add((String) (customFieldValueMap.getOrDefault("ILAB No", "")));
		row.add((String) (customFieldValueMap.getOrDefault("Finalize Flag", "")));
		row.add((String) (customFieldValueMap.getOrDefault("Cost Center", "")));
		row.add((String) (customFieldValueMap.getOrDefault("Fund ID", "")));
		row.add((String) (customFieldValueMap.getOrDefault("MTA Flag", "")));
		row.add((String) (customFieldValueMap.getOrDefault("Distribution Option Desc", "")));
		row.add((String) (customFieldValueMap.getOrDefault("HBC ID", "")));
		row.add((String) (customFieldValueMap.getOrDefault("HBC Committee Approval Date", "")));
		row.add((String) (customFieldValueMap.getOrDefault("Minimum Unit", "")));
		row.add((String) (customFieldValueMap.getOrDefault("MTA Approval Date", "")));
		row.add((String) (customFieldValueMap.getOrDefault("Pickup Arrangement Description","")));
		row.add((String) (customFieldValueMap.getOrDefault("Prospect Flag","")));
		row.add((String) (customFieldValueMap.getOrDefault("Type Description","")));
		row.add((String) (customFieldValueMap.getOrDefault("Restrospect Flag","")));
		row.add((String) (customFieldValueMap.getOrDefault("Specimen Collection Method","")));
		row.add((String) (customFieldValueMap.getOrDefault("Comments","")));
		row.add((String) (customFieldValueMap.getOrDefault("Contact Name","")));
		row.add((String) (customFieldValueMap.getOrDefault("Specimen Usage Description","")));
		row.add((String) (customFieldValueMap.getOrDefault("Waiver Number","")));
		row.add((String) (customFieldValueMap.getOrDefault("Minimum Size","")));
		row.add((String) (customFieldValueMap.getOrDefault("Disease Status","")));
		row.add((String) (customFieldValueMap.getOrDefault("Special Handling","")));
		
		return row.toArray(new String[row.size()]);
	}
	
	private String getDpReceivingSiteName(DistributionProtocol dp) {
		return dp.getDefReceivingSite() != null ? dp.getDefReceivingSite().getName() : "";
	}

	///////////////////////
	//
	// DPR export
	//
	///////////////////////
	
	private void exportDpr(CsvFileWriter dpRFileWriter, Set<DpRequirement> DpRequirements) {
		if (!DpRequirements.isEmpty()) {
			DpRequirements.forEach(dpR -> processDpR(dpRFileWriter, dpR));
		}
	}

	@SuppressWarnings("unchecked")
	private void processDpR(CsvFileWriter dpRFileWriter, DpRequirement dpR) {
		List<Attr> extensions = dpR.getExtension().getAttrs();
		
		if (extensions.isEmpty()) {
			dpRFileWriter.writeNext(getDpRRow(dpR, new ArrayList<String>()));
		}
		
		for (Attr extension : extensions) {
			List<List<Attr>> customFields = (List<List<Attr>>) extension.getValue();
			customFields.forEach(customField -> dpRFileWriter.writeNext(getDpRRow(dpR, getDpRCustomFieldValues(customField))));
		}
	}
	
	private String[] getDpRRow(DpRequirement dpr, List<String> dpRCustomFieldValues) {
		List<String> row = new ArrayList<String>();
		
		row.add(dpr.getSpecimenType());
		row.addAll(splitToMultiple(dpr.getAnatomicSite(), 3, "/")); 
		row.add(getPathologyStatus(dpr));
		row.add(dpr.getQuantity().toString());
		row.add(getDprCost(dpr));
		row.add(dpr.getDistributionProtocol().getShortTitle());
		row.addAll(dpRCustomFieldValues);
		
		return row.toArray(new String[row.size()]);
	}

	private String getPathologyStatus(DpRequirement dpr) {
		return dpr.getPathologyStatuses().isEmpty() ? "" : dpr.getPathologyStatuses().iterator().next();
	}

	private String getDprCost(DpRequirement dpr) {
		return dpr.getCost() != null ? dpr.getCost().toString() : "";
	}

	private List<String> getDpRCustomFieldValues(List<Attr> customField) {
		List<String> customFieldValues = new ArrayList<>();
		
		if (!histologyValuePresent(customField)) {
			customFieldValues.addAll(0, splitToMultiple("", 3, "/"));
			customFieldValues.addAll(customField.stream()
					.map(Attr::getDisplayValue)
					.collect(Collectors.toList()));
			
			return customFieldValues;
		} else {
			customFieldValues.addAll(customField.stream()
					.map(Attr::getDisplayValue)
					.collect(Collectors.toList()));
			String histology = customFieldValues.get(0);
			customFieldValues.remove(0);
			customFieldValues.addAll(0, splitToMultiple(histology, 4, "/"));
			
			return customFieldValues;
		}
	}

	private boolean histologyValuePresent(List<Attr> customFieldValues) {
		for (Attr value : customFieldValues) {
			if (value.getCaption().equals("Histology") && !StringUtils.isEmpty(value.getDisplayValue())) {
				return true;
			}
		}
		
		return false;
	}

	private CsvFileWriter getDpRCSVWriter() {
		String exportFolder = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
		File outputFile = new File(ConfigUtil.getInstance().getDataDir() + File.separatorChar + exportFolder, "Specimen_Request_Details" + ".csv");
		return CsvFileWriter.createCsvFileWriter(outputFile);
	}

	private String[] getDpRHeader() {
		return new String[] {
				"TBRD_SPECIMEN_TYPE_CD",
				"TBRD_SITE_DESC",
				"TBRD_SUB_SITE_DESC",
				"TBRD_SUB2_SITE_DESC",
				"TBRD_CATEGORY_DESC",
				"TBRD_EXPECTED_AMT", 
				"TBRD_BILLING_AMT",
				"TBRD_SOURCE_REQUEST",
				"TBRD_HISTOLOGY_DESC",
				"TBRD_HISTOLOGY_SUB_DESC",
				"TBRD_HISTOLOGY_SUB2_DESC",
				"TBRD_HISTOLOGY_SUB3_DESC",
				"TBRD_QUALITY_DESC",
				"TBRD_UNIT_DESC",
				"TBRD_NOTES"
		};
	}
	
	///////////////////////
	//
	// Distribution Orders
	//
	///////////////////////
	
	private CsvFileWriter getDoCSVWriter() {
		String exportFolder = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
		File outputFile = new File(ConfigUtil.getInstance().getDataDir() + File.separatorChar + exportFolder, "Distribution" + ".csv");
		return CsvFileWriter.createCsvFileWriter(outputFile);
	}
	
	private String[] getDoHeader() {
		return new String[] {
				"TBDS_SPECIMEN_REQUEST_ID",
				"TBDS_DISTRIBUTION_DT",
				"TBDS_SOURCE_REQUEST",
				"TBDS_BILLING_AMT",
				"SPECIMEN_LABEL",
				"TBDS_BILLING_DT"
		};
	}

	private void exportDOs(CsvFileWriter doFileWriter, Set<DistributionOrder> distributionOrders) {
		if (!distributionOrders.isEmpty()) {
			distributionOrders.forEach(distributionOrder -> processDistributionOrders(doFileWriter, distributionOrder.getOrderItems()));
		}
	}
	
	private void processDistributionOrders(CsvFileWriter doFileWriter, Set<DistributionOrderItem> orderItems) {
		if (!orderItems.isEmpty()) {
			orderItems.forEach(item -> doFileWriter.writeNext(getDoRow(item)));
		}
	}

	private String[] getDoRow(DistributionOrderItem item) {
		List<String> row = new ArrayList<String>();
		
		row.add(item.getOrder().getDistributionProtocol().getShortTitle());
		row.add(item.getOrder().getExecutionDate().toString());
		row.add(item.getOrder().getDistributionProtocol().getShortTitle());
		row.add(getItemCost(item));
		row.add(item.getSpecimen().getLabel());
		row.addAll(getCustomFieldValues(item.getOrder()));
		
		return row.toArray(new String[row.size()]);
	}
	
	private String getItemCost(DistributionOrderItem item) {
		return item.getCost() != null ? item.getCost().toString() : "";
	}
}	
