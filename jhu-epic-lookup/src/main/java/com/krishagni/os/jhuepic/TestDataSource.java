package com.krishagni.os.jhuepic;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedParticipant;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.matching.ParticipantLookupLogic;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;

@Configurable
public class TestDataSource implements ParticipantLookupLogic {

	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private DaoFactory daoFactory;

	@Override
	public List<MatchedParticipant> getMatchingParticipants(ParticipantDetail criteria) {
		if (StringUtils.isBlank(criteria.getEmpi())) {
			return Collections.emptyList();
		}

		Participant localDb = daoFactory.getParticipantDao().getByEmpi(criteria.getEmpi());
		ParticipantDetail local = null;
		if (localDb != null) {
			local = ParticipantDetail.from(localDb, false);
		}

		List<Object[]> rows = (List<Object[]>)sessionFactory.getCurrentSession()
			.createSQLQuery("select first_name, last_name, dob, empi from test_participants where empi = :empi")
			.setString("empi", criteria.getEmpi())
			.list();

		if (CollectionUtils.isNotEmpty(rows)) {
			if (local == null) {
				local = new ParticipantDetail();
				//local.setSource("TEST");
			}

			local.setFirstName((String)rows.get(0)[0]);
			local.setLastName((String)rows.get(0)[1]);
			local.setBirthDate((Date)rows.get(0)[2]);
			local.setEmpi((String)rows.get(0)[3]);
		}

		if (local == null) {
			return Collections.emptyList();
		}

		return Collections.singletonList(new MatchedParticipant(local, Collections.singletonList("empi")));
	}
}
