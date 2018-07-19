package com.krishagni.openspecimen.msk;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.SessionFactory;
import org.hibernate.type.StringType;
import org.springframework.context.ApplicationListener;

import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.VisitSavedEvent;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Utility;

//
// operation date <= visit date == path date <= resection date/time
//
public class VisitDatesValidation implements ApplicationListener<VisitSavedEvent> {

	@Override
	public void onApplicationEvent(VisitSavedEvent event) {
		Visit visit = event.getEventData();
		if (!visit.isCompleted() || !visit.isActive()) {
			return;
		}

		String visitName = visit.getName();
		Date visitDate   = Utility.chopTime(visit.getVisitDate());
		Date pathDate    = CustomDateFields.getInstance().getPathDate(visit);
		Date opDate      = CustomDateFields.getInstance().getOperationDate(visit);

		if (visitDate != null && pathDate != null && !DateUtils.isSameDay(visitDate, pathDate)) {
			throw OpenSpecimenException.userError(
				MskError.PROC_DT_NE_PATH_DT,
				visitName, toDateStr(visitDate), toDateStr(pathDate));
		}

		if (visitDate != null && opDate != null && !isSameDayOrAfter(visitDate, opDate)) {
			throw OpenSpecimenException.userError(
				MskError.PROC_DT_LT_OP_DT,
				visitName, toDateStr(visitDate), toDateStr(opDate));
		}

		if (pathDate != null && opDate != null && !isSameDayOrAfter(pathDate, opDate)) {
			throw OpenSpecimenException.userError(
				MskError.PATH_DT_LT_OP_DT,
				visitName, toDateStr(pathDate), toDateStr(opDate));
		}

		List<String> invCollDateSpmns = getSpecimensCollectedBeforeProcurementDate(visit);
		if (!invCollDateSpmns.isEmpty()) {
			throw OpenSpecimenException.userError(
				MskError.SPMN_COLL_DT_LT_PROC_DT,
				visitName, toDateStr(visitDate), invCollDateSpmns);
		}
	}

	@SuppressWarnings("unchecked")
	private List<String> getSpecimensCollectedBeforeProcurementDate(Visit visit) {
		return (List<String>) getSessionFactory().getCurrentSession().createSQLQuery(GET_INV_COLL_DATE_PRIMARY_SPMNS)
			.addScalar("label", StringType.INSTANCE)
			.setLong("visitId", visit.getId())
			.setDate("visitDate", visit.getVisitDate())
			.setMaxResults(5)
			.list();
	}

	private boolean isSameDayOrAfter(Date d1, Date d2) {
		return DateUtils.isSameDay(d1, d2) || d1.after(d2);
	}

	private SessionFactory getSessionFactory() {
		return OpenSpecimenAppCtxProvider.getBean("sessionFactory");
	}

	private String toDateStr(Date date) {
		return Utility.getDateString(date);
	}

	private static final String GET_INV_COLL_DATE_PRIMARY_SPMNS =
		"select " +
		"  s.label " +
		"from " +
		"  catissue_specimen s " +
		"  inner join catissue_form_record_entry re on re.object_id = s.identifier " +
		"  inner join catissue_form_context fc on fc.identifier = re.form_ctxt_id " +
		"  inner join dyextn_containers f on f.identifier = fc.container_id " +
		"  inner join catissue_coll_event_param ce on ce.identifier = re.record_id " +
		"where " +
		"  s.lineage = 'New' and " +
		"  s.collection_status = 'Collected' and " +
		"  re.activity_status = 'ACTIVE' and " +
		"  fc.deleted_on is null and " +
		"  f.name = 'SpecimenCollectionEvent' and " +
		"  s.activity_status != 'Disabled' and " +
		"  s.specimen_collection_group_id = :visitId and " +
		"  ce.event_timestamp < :visitDate";
}
