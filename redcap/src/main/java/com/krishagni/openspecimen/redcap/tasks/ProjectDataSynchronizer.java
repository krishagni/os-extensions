package com.krishagni.openspecimen.redcap.tasks;

import java.util.Calendar;

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
import com.krishagni.openspecimen.redcap.events.UpdateDataOp;
import com.krishagni.openspecimen.redcap.repository.ProjectDao;
import com.krishagni.openspecimen.redcap.services.ProjectService;

@Configurable
public class ProjectDataSynchronizer implements ScheduledTask {
		
	@Autowired
	private DaoFactory daoFactory;
	
	@Autowired
	private ProjectService projectSvc;
	
	@Autowired
	private ProjectDao projectDao;
	
	@Override
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		try {
			loadProjectsData();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	private void loadProjectsData() {
		System.err.println("Loading projects data");
		
		setUserContext();
				
		UpdateDataOp op = new UpdateDataOp();
		op.setEndTs(Calendar.getInstance().getTime());
		
		ResponseEvent<Void> resp = projectSvc.updateData(new RequestEvent<UpdateDataOp>(op));
		if (resp.isSuccessful()) {
			System.err.println("Successfully queued projects data load");
		}
	}
	
	@PlusTransactional
	private void setUserContext() {
		User user = daoFactory.getUserDao().getSystemUser();		
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, "secret", user.getAuthorities()));		
	}	
}