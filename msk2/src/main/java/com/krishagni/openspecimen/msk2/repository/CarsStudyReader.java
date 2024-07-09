package com.krishagni.openspecimen.msk2.repository;

import java.io.Closeable;

import com.krishagni.openspecimen.msk2.events.CarsStudyDetail;

public interface CarsStudyReader extends Closeable {
	CarsStudyDetail next();
}
