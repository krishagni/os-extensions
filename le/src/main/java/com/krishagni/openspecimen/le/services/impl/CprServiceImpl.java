package com.krishagni.openspecimen.le.services.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantUtil;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.LabelGenerator;
import com.krishagni.openspecimen.le.events.BulkParticipantRegDetail;
import com.krishagni.openspecimen.le.events.ParticipantRegDetail;
import com.krishagni.openspecimen.le.services.CprService;

public class CprServiceImpl implements CprService {
	
	private DaoFactory daoFactory;
	
	private LabelGenerator labelGenerator;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}
	
	public void setLabelGenerator(LabelGenerator labelGenerator) {
		this.labelGenerator = labelGenerator;
	}


	@Override
	@PlusTransactional	
	public ResponseEvent<BulkParticipantRegDetail> registerParticipants(RequestEvent<BulkParticipantRegDetail> req) {		
		try {
			BulkParticipantRegDetail detail = req.getPayload();
			
			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(detail.getCpId());
			if (cp == null) {
				return ResponseEvent.userError(CpErrorCode.NOT_FOUND);
			}
			
			List<ParticipantRegDetail> result = new ArrayList<ParticipantRegDetail>();
			for (ParticipantRegDetail regDetail : detail.getRegistrations()) {
				regDetail = registerParticipant(cp, regDetail, ose);
				ose.checkAndThrow();
				
				result.add(regDetail);
			}
			
			return ResponseEvent.response(new BulkParticipantRegDetail(detail.getCpId(), result));			
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);			
		} catch (Exception e) {
			return ResponseEvent.serverError(e);			
		}
	}
	
	private ParticipantRegDetail registerParticipant(CollectionProtocol cp, ParticipantRegDetail detail, OpenSpecimenException ose) {
		String empi = detail.getEmpi();
		if (StringUtils.isBlank(empi)) {
			ose.addError(ParticipantErrorCode.EMPI_REQUIRED);
			return null;
		}
		
		if (!ParticipantUtil.isValidMpi(empi, ose)) {
			return null;
		}
		
		String ppid = detail.getPpid();
		if (StringUtils.isBlank(ppid)) {
			ose.addError(CprErrorCode.PPID_REQUIRED);
			return null;
		}

		Participant participant = daoFactory.getParticipantDao().getByEmpi(empi);
		if (participant == null) {
			participant = new Participant();
			participant.setEmpi(empi);
		}
		
		CollectionProtocolRegistration cpr = daoFactory.getCprDao().getCprByPpid(cp.getId(), ppid);
		if (cpr != null && !cpr.getParticipant().equals(participant)) {
			ose.addError(CprErrorCode.DUP_PPID, ppid);
			return detail;
		} 
		
		if (cpr == null && participant.getId() != null) {
			cpr = daoFactory.getCprDao().getCprByParticipantId(cp.getId(), participant.getId());
		}
				
		if (cpr != null) {
			return ParticipantRegDetail.from(cpr);
		}
		
		cpr = new CollectionProtocolRegistration();
		cpr.setPpid(ppid);
		cpr.setCollectionProtocol(cp);
		cpr.setParticipant(participant);
			
		Date regDate = detail.getRegDate();
		if (regDate == null) {
			regDate = Calendar.getInstance().getTime();
		}			
		cpr.setRegistrationDate(regDate);
		
		ensureValidAndUniquePpid(cpr, ose);
			
		if (participant.getId() == null) {
			daoFactory.getParticipantDao().saveOrUpdate(participant);
		}
		
		AccessCtrlMgr.getInstance().ensureUpdateCprRights(cpr);		
		daoFactory.getCprDao().saveOrUpdate(cpr);
		return ParticipantRegDetail.from(cpr);
	}
	
	private void ensureValidAndUniquePpid(CollectionProtocolRegistration cpr, OpenSpecimenException ose) {
		CollectionProtocol cp = cpr.getCollectionProtocol();
		boolean ppidReq = cp.isManualPpidEnabled() || StringUtils.isBlank(cp.getPpidFormat());
		
		String ppid = cpr.getPpid();
		if (StringUtils.isBlank(ppid)) {
			if (ppidReq) {
				ose.addError(CprErrorCode.PPID_REQUIRED);
			}
			
			return;
		}
		
		if (StringUtils.isNotBlank(cp.getPpidFormat())) {
			//
			// PPID format is specified
			//
			if (!cp.isManualPpidEnabled()) {
				ose.addError(CprErrorCode.MANUAL_PPID_NOT_ALLOWED);
				return;
			}
			
			if (!labelGenerator.validate(cp.getPpidFormat(), cpr, ppid)) {
				ose.addError(CprErrorCode.INVALID_PPID, ppid);
				return;
			}
		}
		
		if (daoFactory.getCprDao().getCprByPpid(cp.getId(), ppid) != null) {
			ose.addError(CprErrorCode.DUP_PPID, ppid);
		}
	}
}
