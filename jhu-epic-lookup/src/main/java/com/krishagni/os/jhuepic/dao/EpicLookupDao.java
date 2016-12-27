package com.krishagni.os.jhuepic.dao;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;

public class EpicLookupDao {

	private SessionFactory sessionFactory;
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@SuppressWarnings("unchecked")
	public List<Participant> getLocalMatchingByEmpiMrn(String empi) {
		return sessionFactory.getCurrentSession()
				.createQuery(GET_MATCHING_BY_EMPI_MRN)
				.setString("empi", empi)
				.setString("mrn", empi)
				.list();
	}
	
	@SuppressWarnings("unchecked")
	public List<Participant> getLocalMatchingByPmi(List<PmiDetail> pmis) {
		Criteria query = getByPmisQuery(pmis);
		if (query == null) {
			return Collections.emptyList();
		}
		
		return query.list();
	}
	
	private Criteria getByPmisQuery(List<PmiDetail> pmis) {
		Criteria query = sessionFactory.getCurrentSession().createCriteria(Participant.class)
				.createAlias("pmis", "pmi")
				.createAlias("pmi.site", "site");
		
		Disjunction junction = Restrictions.disjunction();
		
		boolean added = false;
		for (PmiDetail pmi : pmis) {
			if (StringUtils.isBlank(pmi.getSiteName()) || StringUtils.isBlank(pmi.getMrn())) {
				continue;
			}
			
			junction.add(
					Restrictions.and(Restrictions.isNotNull("empi"),
					Restrictions.and(
							Restrictions.eq("pmi.medicalRecordNumber", pmi.getMrn()),
							Restrictions.eq("site.name", pmi.getSiteName()))));
			
			added = true;
		}
		
		if (!added) {
			return null;
		}
		return query.add(junction);						
		
	}
	
	private String GET_MATCHING_BY_EMPI_MRN = 
			" select distinct p from " +
			"   com.krishagni.catissueplus.core.biospecimen.domain.Participant p " +
			"   left join p.pmis pmi " +
			" where " +
			"   p.empi is not null and (p.empi = :empi or pmi.medicalRecordNumber = :mrn)";
	
}
