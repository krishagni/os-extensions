package com.krishagni.openspecimen.redcap.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.openspecimen.redcap.events.UpdateInstrumentsOp;
import com.krishagni.openspecimen.redcap.repository.ProjectDao;
import com.krishagni.openspecimen.redcap.services.ProjectService;

@Configurable
public class ProjectMetadataSynchronizer implements ScheduledTask {

	@Autowired
	private ProjectService projectSvc;

	@Autowired
	private ProjectDao projectDao;
	
	@Autowired
	private DaoFactory daoFactory;
		
	@Override
	public void doJob(ScheduledJobRun jobRun) 
	throws Exception {
		updateInstruments();
	}
	
	private void updateInstruments() {
		try {
			setUserContext();
	
			UpdateInstrumentsOp op = new UpdateInstrumentsOp();
			ResponseEvent<Void> resp = projectSvc.updateInstruments(new RequestEvent<UpdateInstrumentsOp>(op));
			if (resp.isSuccessful()) {
				System.err.println("Successfully updated project instruments");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@PlusTransactional
	private void setUserContext() {
		User user = daoFactory.getUserDao().getSystemUser();		
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, "secret", user.getAuthorities()));		
	}
}
