package com.krishagni.openspecimen.msk2.repository;

import com.krishagni.openspecimen.msk2.events.CarsStudyDetail;

public interface CarsStudyReader extends AutoCloseable {
	CarsStudyDetail next();
}
