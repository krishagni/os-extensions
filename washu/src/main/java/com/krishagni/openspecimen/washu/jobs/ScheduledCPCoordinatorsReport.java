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
public class ScheduledCPCoordinatorsReport implements ScheduledTask {
	private final static String WASHU_CP_COORD_REPORT = "washu_cp_coord_report";

	@Autowired
	private DaoFactory daoFactory;

	@Override
	public void doJob(ScheduledJobRun job) throws Exception {
		List<Long> cpIds = getAllCpIds();
		for (Long cpId : cpIds) {
			notifyPiForCp(cpId);
		}
	}

	@PlusTransactional
	private void notifyPiForCp(Long cpId) {
		CollectionProtocol cp = getCpById(cpId);
		String piEmail = cp.getPrincipalInvestigator().getEmailAddress();
		Map<String, Object> mailProps = getProps(cp);

		EmailUtil.getInstance().sendEmail(WASHU_CP_COORD_REPORT, new String[] { piEmail }, null, mailProps);
	}

	@PlusTransactional
	private List<Long> getAllCpIds() {
		return daoFactory.getCollectionProtocolDao().getAllCpIds();
	}

	private CollectionProtocol getCpById(Long cpId) {
		return daoFactory.getCollectionProtocolDao().getById(cpId);
	}

	private Map<String, Object> getProps(CollectionProtocol cp) {
		Map<String, Object> props = new HashMap<>();
		String helpdeskEmail = ConfigUtil.getInstance().getStrSetting("washu", "washu_helpdesk_email", "");
		String contactEmail = ConfigUtil.getInstance().getStrSetting("washu", "washu_contact_email", "");

		props.put("cpShortTitle", cp.getShortTitle());
		props.put("cpCoords", cp.getCoordinators().stream().map(User::formattedName).collect(Collectors.joining(", ")));
		props.put("helpdeskEmail", helpdeskEmail);
		props.put("contactEmail", contactEmail);
		return props;
	}
}
