package com.krishagni.openspecimen.msk;

import java.util.Date;

import org.springframework.context.ApplicationListener;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenSavedEvent;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Utility;

//
// visit date <= resection date/time <= accession date/time <= receipt date/time <= process date/time
//
public class SpecimenDatesValidation implements ApplicationListener<SpecimenSavedEvent> {

	@Override
	public void onApplicationEvent(SpecimenSavedEvent event) {
		Specimen specimen = event.getEventData();
		if (!specimen.isCollected() || !specimen.isActive()) {
			return;
		}

		String label = specimen.getLabel();

		Date receiptDate = null;
		if (specimen.isPrimary()) {
			Date visitDate = Utility.chopTime(specimen.getVisit().getVisitDate());
			Date resectionDate = specimen.getCollectionEvent().getTime();
			if (resectionDate.before(visitDate)) {
				throw OpenSpecimenException.userError(
					MskError.COLL_DT_LT_PROC_DT,
					label, toDateTimeStr(resectionDate), toDateStr(visitDate)
				);
			}

			Date accessionedDate = CustomDateFields.getInstance().getAccessionedDate(specimen);
			if (accessionedDate != null && accessionedDate.before(resectionDate)) {
				throw OpenSpecimenException.userError(
					MskError.ACC_DT_LT_COLL_DT,
					label, toDateTimeStr(accessionedDate), toDateTimeStr(resectionDate)
				);
			}

			receiptDate = specimen.getReceivedEvent().getTime();
			if (receiptDate.before(resectionDate)) {
				throw OpenSpecimenException.userError(
					MskError.RECV_DT_LT_COLL_DT,
					label, toDateTimeStr(receiptDate), toDateTimeStr(resectionDate)
				);
			}

			if (accessionedDate != null && receiptDate.before(accessionedDate)) {
				throw OpenSpecimenException.userError(
					MskError.RECV_DT_LT_ACC_DT,
					label, toDateTimeStr(receiptDate), toDateTimeStr(accessionedDate)
				);
			}
		} else {
			receiptDate = specimen.getParentSpecimen().getCreatedOn();
		}

		Date processTime = specimen.getCreatedOn();
		if (processTime.before(receiptDate)) {
			throw OpenSpecimenException.userError(
				MskError.CRET_DT_LT_RECV_DT,
				label, toDateTimeStr(processTime), toDateTimeStr(receiptDate)
			);
		}
	}

	private String toDateStr(Date date) {
		return Utility.getDateString(date);
	}

	private String toDateTimeStr(Date date) {
		return Utility.getDateTimeString(date);
	}
}
