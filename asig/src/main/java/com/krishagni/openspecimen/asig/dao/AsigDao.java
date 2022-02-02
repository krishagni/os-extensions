
package com.krishagni.openspecimen.asig.dao;

import java.io.File;
import java.util.List;

import com.krishagni.openspecimen.asig.events.PatientDetail;
import com.krishagni.openspecimen.asig.events.ClinicDetail;
import com.krishagni.openspecimen.asig.events.AsigUserDetail;

public interface AsigDao {

    public List<PatientDetail> getAsigPatientDetail();
		
	public List<ClinicDetail> getAsigClinicDetail();
		
	public List<AsigUserDetail> getAsigUserDetail();
		
	public void insertPatientMapping(Long asigId, Long osId);
		
	public void insertSiteMapping(Long asigId, Long osId);
		
	public void insertUserMapping(Long AsigId, Long osId);
		
	public Long getOsPatientId(Long asigId);
		
	public Long getOsSiteId(Long asigSiteId);
		
	public Long getOsUserId(Long asigSiteId);
		
	public String getSiteName(Long aisgClinicId);

	void importData(File file, String entity);
}
