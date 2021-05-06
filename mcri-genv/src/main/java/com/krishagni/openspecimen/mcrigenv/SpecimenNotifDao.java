package com.krishagni.openspecimen.mcrigenv;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class SpecimenNotifDao extends AbstractDao<SpecimenNotif> {

	@Override
	public Class<SpecimenNotif> getType() {
		return SpecimenNotif.class;
	}

	public SpecimenNotif getBySpecimen(Long specimenId) {
		return (SpecimenNotif) getCurrentSession().getNamedQuery(GET_BY_SPMN)
			.setParameter("specimenId", specimenId)
			.uniqueResult();
	}

	private static final String FQN = SpecimenNotif.class.getName();

	private static final String GET_BY_SPMN = FQN + ".getBySpecimen";
}
