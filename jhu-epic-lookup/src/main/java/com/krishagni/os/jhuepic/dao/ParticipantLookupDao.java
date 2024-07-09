package com.krishagni.os.jhuepic.dao;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
import com.krishagni.catissueplus.core.common.repository.Query;

public class ParticipantLookupDao {

	private SessionFactory sessionFactory;
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public List<Participant> getByEmpiMrn(String empi) {
		return Query.createQuery(sessionFactory.getCurrentSession(), GET_BY_EMPI_MRN_HQL, Participant.class)
			.setParameter("empi", empi)
			.setParameter("mrn", empi)
			.list();
	}

	public List<Participant> getByPmi(List<PmiDetail> pmis) {
		Criteria<Participant> query = getByPmisQuery(pmis);
		if (query == null) {
			return Collections.emptyList();
		}
		
		return query.list();
	}
	
	private Criteria<Participant> getByPmisQuery(List<PmiDetail> pmis) {
		Criteria<Participant> query = Criteria.create(sessionFactory.getCurrentSession(), Participant.class, "p");
		query.join("pmis", "pmi")
			.join("pmi.site", "site")
			.add(query.isNotNull("p.empi"));

		boolean added = false;
		Disjunction junction = query.disjunction();
		for (PmiDetail pmi : pmis) {
			if (StringUtils.isBlank(pmi.getSiteName()) || StringUtils.isBlank(pmi.getMrn())) {
				continue;
			}
			
			junction.add(
				query.and(
					query.eq("pmi.medicalRecordNumber", pmi.getMrn()),
					query.eq("site.name", pmi.getSiteName())));
			
			added = true;
		}
		
		if (!added) {
			return null;
		}

		return query.add(junction);
	}
	
	private static final String GET_BY_EMPI_MRN_HQL =
			" select " +
			"   distinct p " +
			" from " +
			"   com.krishagni.catissueplus.core.biospecimen.domain.Participant p " +
			"   left join p.pmis pmi " +
			" where " +
			"   p.empi is not null and " +
			"  (p.empi = :empi or pmi.medicalRecordNumber = :mrn)";
	
}
