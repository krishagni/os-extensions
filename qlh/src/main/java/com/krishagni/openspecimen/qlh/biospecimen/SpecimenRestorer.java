package com.krishagni.openspecimen.qlh.biospecimen;

import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.type.LongType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.events.StorageLocationSummary;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenChildrenEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenCollectionReceiveDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.NameValuePair;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.rbac.common.errors.RbacErrorCode;

@Configurable
public class SpecimenRestorer {
	private static final LogUtil logger = LogUtil.getLogger(SpecimenRestorer.class);

	@Autowired
	private ThreadPoolTaskExecutor taskExecutor;

	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private SpecimenService spmnSvc;

	public void restoreSpecimens(SpecimenRestoreDetail input) {
		User currentUser = AuthUtil.getCurrentUser();
		if (!currentUser.isAdmin()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		String ipAddress = AuthUtil.getRemoteAddr();
		taskExecutor.execute(
			() -> {
				AuthUtil.setCurrentUser(currentUser, ipAddress);
				restoreSpecimens0(input);
			}
		);
	}

	private void restoreSpecimens0(SpecimenRestoreDetail input) {
		long startTime = System.currentTimeMillis();
		logger.info("Restoring specimens for the criteria: " + Utility.objectToJson(input));

		List<Long> specimenIds = getSpecimensToRestore(input);
		logger.info("Count of specimens identified for restore: " + specimenIds.size());
		logger.info("Specimen IDs: " + StringUtils.join(specimenIds, ", "));

		int total = 0, error = 0;
		for (Long specimenId : getSpecimensToRestore(input)) {
			try {
				restoreSpecimen(specimenId, input.getOverwrittenBy());
			} catch (Exception e) {
				logger.error("Encountered error when restoring the specimen " + specimenId + ". Error = " + Utility.getErrorMessage(e), e);
				++error;
			}

			++total;
		}

		long endTime = System.currentTimeMillis();
		logger.info("Updated: " + (total - error) + ", Error: " + error + ", Time Taken: " + ((endTime - startTime) / 1000) + " seconds");
	}

	@PlusTransactional
	private List<Long> getSpecimensToRestore(SpecimenRestoreDetail input) {
		Session session = sessionFactory.getCurrentSession();
		return (List<Long>) session.createNativeQuery(GET_OVERWRITTEN_SPMN_IDS_SQL)
			.addScalar("specimenId", LongType.INSTANCE)
			.setParameter("overwrittenBy", input.getOverwrittenBy())
			.setParameter("from", input.getFrom())
			.setParameter("to", input.getTo())
			.list();
	}

	@PlusTransactional
	private void restoreSpecimen(Long specimenId, String overwrittenBy) {
		long startTime = System.currentTimeMillis();
		logger.info("Restoring specimen: " + specimenId);

		Session session = sessionFactory.getCurrentSession();
		Long revision = (Long) session.createNativeQuery(GET_LATEST_OVERWRITTEN_SPMN_REV_SQL)
			.addScalar("revision", LongType.INSTANCE)
			.setParameter("specimenId", specimenId)
			.setParameter("overwrittenBy", overwrittenBy)
			.uniqueResult();

		logger.info("Retrieving the snapshot of specimen: " + specimenId + " at revision: " + revision);
		AuditReader reader = AuditReaderFactory.get(session);
		Specimen specimen = reader.find(Specimen.class, specimenId, revision);

		logger.info("Restoring the snapshot of specimen: " + specimenId + " at revision: " + revision);
		ResponseEvent.unwrap(spmnSvc.updateSpecimen(RequestEvent.wrap(toDetail(specimen))));

		long endTime = System.currentTimeMillis();
		logger.info("Restored the snapshot of specimen: " + specimenId + " at revision: " + revision + " in " + (endTime - startTime) + " ms");
	}

	private SpecimenDetail toDetail(Specimen specimen) {
		SpecimenDetail result = new SpecimenDetail();
		result.setId(specimen.getId());
		if (specimen.getSpecimenRequirement() != null) {
			result.setReqId(specimen.getSpecimenRequirement().getId());
		}

		if (specimen.getVisit() != null && specimen.getVisit().getCpEvent() != null) {
			result.setEventId(specimen.getVisit().getCpEvent().getId());
		}

		result.setLabel(specimen.getLabel());
		result.setAdditionalLabel(specimen.getAdditionalLabel());
		result.setBarcode(specimen.getBarcode());
		result.setType(PermissibleValue.getValue(specimen.getSpecimenType()));
		result.setSpecimenClass(PermissibleValue.getValue(specimen.getSpecimenClass()));
		result.setLineage(specimen.getLineage());
		result.setAnatomicSite(PermissibleValue.getValue(specimen.getTissueSite()));
		result.setLaterality(PermissibleValue.getValue(specimen.getTissueSide()));
		result.setStatus(specimen.getCollectionStatus());
		result.setPathology(PermissibleValue.getValue(specimen.getPathologicalStatus()));
		result.setInitialQty(specimen.getInitialQuantity());
		result.setAvailableQty(specimen.getAvailableQuantity());
		result.setConcentration(specimen.getConcentration());
		if (specimen.getParentSpecimen() != null) {
			result.setParentId(specimen.getParentSpecimen().getId());
			result.setParentLabel(specimen.getParentSpecimen().getLabel());
		}

		StorageLocationSummary location = null;
		StorageContainerPosition position = specimen.getPosition();
		if (position == null) {
			location = new StorageLocationSummary();
			location.setId(-1L);
		} else {
			location = StorageLocationSummary.from(position);
		}
		result.setStorageLocation(location);

		result.setActivityStatus(specimen.getActivityStatus());
		result.setAvailabilityStatus(specimen.getAvailabilityStatus());
		result.setCreatedOn(specimen.getCreatedOn());
		result.setVisitId(specimen.getVisit().getId());
		result.setVisitName(specimen.getVisit().getName());
		result.setVisitStatus(specimen.getVisit().getStatus());
		result.setCprId(specimen.getRegistration().getId());
		result.setCpId(specimen.getCollectionProtocol().getId());
		result.setFreezeThawCycles(specimen.getFreezeThawCycles());
		result.setImageId(specimen.getImageId());
		result.setExternalIds(specimen.getExternalIds().stream()
			.map(externalId -> NameValuePair.create(externalId.getName(), externalId.getValue()))
			.collect(Collectors.toList()));

		SpecimenCollectionReceiveDetail collRecvDetail = specimen.getCollRecvDetails();
		if (collRecvDetail != null) {
			result.setCollector(UserSummary.from(collRecvDetail.getCollector()));
			result.setCollectionContainer(collRecvDetail.getCollContainer());
			result.setCollectionDate(collRecvDetail.getCollTime());
		} else if (specimen.isPrimary() && specimen.getSpecimenRequirement() != null) {
			result.setCollector(UserSummary.from(specimen.getSpecimenRequirement().getCollector()));
			result.setCollectionContainer(PermissibleValue.getValue(specimen.getSpecimenRequirement().getCollectionContainer()));
		}

		SpecimenChildrenEvent parentEvent = specimen.getParentEvent();
		if (parentEvent != null) {
			result.setCreatedBy(UserSummary.from(parentEvent.getUser()));
			if (result.getCreatedOn() == null) {
				result.setCreatedOn(parentEvent.getTime());
			}
		}

		result.setBiohazards(PermissibleValue.toValueSet(specimen.getBiohazards()));
		result.setComments(specimen.getComment());
		result.setReserved(specimen.isReserved());
		result.setUid(specimen.getUid());
		result.setParentUid(specimen.getParentUid());
		result.setCheckedOut(specimen.getCheckoutPosition() != null);
		result.setCheckoutPosition(StorageLocationSummary.from(specimen.getCheckoutPosition()));
		return result;
	}

	private static final String GET_OVERWRITTEN_SPMN_IDS_SQL =
		"select " +
		"  distinct t.specimenId " +
		"from (" +
		"  select " +
		"    s1.identifier as specimenId " +
		"  from " +
		"    catissue_specimen_aud s1 " +
		"    inner join catissue_specimen_aud s2 on s1.identifier = s2.identifier " +
		"    inner join os_revisions r1 on s1.rev = r1.rev " +
		"    inner join catissue_user u1 on u1.identifier = r1.user_id " +
		"    inner join os_revisions r2 on s2.rev = r2.rev " +
		"    inner join catissue_user u2 on u2.identifier = r2.user_id " +
		"  where " +
		"    u1.login_name != :overwrittenBy and u2.login_name = :overwrittenBy and " +
		"    r1.revtstmp < r2.revtstmp and " +
		"    r2.revtstmp >= :from and r2.revtstmp <= :to " +
		") t " +
		"order by " +
		"  t.specimenId asc";

	private static final String GET_LATEST_OVERWRITTEN_SPMN_REV_SQL =
		"select " +
		"  max(s.rev) as revision " +
		"from " +
		"  catissue_specimen_aud s " +
		"  inner join os_revisions r on r.rev = s.rev " +
		"  inner join catissue_user u on u.identifier = r.user_id " +
		"where " +
		"  s.identifier = :specimenId and " +
		"  u.login_name != :overwrittenBy";
}
