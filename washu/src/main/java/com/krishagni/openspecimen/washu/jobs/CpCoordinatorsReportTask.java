package com.krishagni.openspecimen.washu.jobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;

@Configurable
public class CpCoordinatorsReportTask implements ScheduledTask {
	private final static String CP_COORD_REPORT = "washu_cp_coord_report";

	@Autowired
	private DaoFactory daoFactory;

	@Override
	public void doJob(ScheduledJobRun job) throws Exception {
		List<Long> cpIds = getAllCpIds();
		int startAt = 0;
		int maxResults = 25;

		processCpIds(cpIds, startAt, maxResults);
	}

	@PlusTransactional
	private void processCpIds(List<Long> cpIds, int startAt, int maxResults) {
		while (true) {
			List<CollectionProtocol> cps = getCpByIds(cpIds.stream().skip(startAt).limit(maxResults).collect(Collectors.toList()));

			for (CollectionProtocol cp : cps) {
				notifyPiForCp(cp);
			}

			if (cps.size() != maxResults) {
				break;
			}

			startAt += maxResults;
		}
	}

	private void notifyPiForCp(CollectionProtocol cp) {
		String piEmail = cp.getPrincipalInvestigator().getEmailAddress();
		Map<String, Object> mailProps = getProps(cp);

		EmailUtil.getInstance().sendEmail(CP_COORD_REPORT, new String[] { piEmail }, null, mailProps);
	}

	@PlusTransactional
	private List<Long> getAllCpIds() {
		return daoFactory.getCollectionProtocolDao().getAllCpIds();
	}

	private List<CollectionProtocol> getCpByIds(List<Long> cpIds) {
		return daoFactory.getCollectionProtocolDao().getByIds(cpIds);
	}

	private Map<String, Object> getProps(CollectionProtocol cp) {
		Map<String, Object> props = new HashMap<>();
		String openspecimenAccEmail = ConfigUtil.getInstance().getStrSetting("email", "account_id", "");

		props.put("cpShortTitle", cp.getShortTitle());
		props.put("cpCoords", cp.getCoordinators().stream().map(User::formattedName).collect(Collectors.joining(", ")));
		props.put("helpdeskEmail", openspecimenAccEmail);
		props.put("contactEmail", openspecimenAccEmail);
		return props;
	}
}
