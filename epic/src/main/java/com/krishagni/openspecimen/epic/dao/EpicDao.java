package com.krishagni.openspecimen.epic.dao;

import java.util.List;
import java.util.Set;

import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierResponseDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.openspecimen.epic.events.CprDetail;
import com.krishagni.openspecimen.epic.events.EpicParticipantDetail;


public interface EpicDao {

	public List<EpicParticipantDetail> getEpicParticipantDetails();
	
	public Long getOsParticipantId(String sourcePartId, String partSource);
	
	public Set<String> getRace(String sourcePartId, String partSource);
	
	public void insertMapping(Long origPartId, String sourcePartId, String partSource);
	
	public void updateMapping(Long origPartId, String sourcePartId, String partSource);
	
	public List<PmiDetail> getPmis(String sourcePartId, String partSource);
	
	public List<CprDetail> getCprDetails(String sourcePartId, String partSource);
	
	public List<ConsentTierResponseDetail> getConsentDetails(String sourcePartId, String partSource, String irbID);
	
	public Long getCpIdByIrbId(String irbId);
	
	public Long getCprId(Long participantId, Long cpId);
	
	public boolean hasSpecimens(Long participantId);
	
	public void updateAuditLog(String staginPartIdSource, String epicParticipantId, String successMsg, String errorMsg);
	
}
