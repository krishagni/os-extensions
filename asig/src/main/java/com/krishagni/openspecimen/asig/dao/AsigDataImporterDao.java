package com.krishagni.openspecimen.asig.dao;

import java.util.List;

import com.krishagni.openspecimen.asig.events.AsigUserDetail;
import com.krishagni.openspecimen.asig.events.ClinicDetail;
import com.krishagni.openspecimen.asig.events.PatientDetail;


public interface AsigDataImporterDao {

	List<ClinicDetail> getClinicDetail();

	List<AsigUserDetail> getUserDetail();

	List<PatientDetail> getPatients();
}
