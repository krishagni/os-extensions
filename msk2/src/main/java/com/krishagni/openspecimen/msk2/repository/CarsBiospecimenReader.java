package com.krishagni.openspecimen.msk2.repository;

import java.io.Closeable;

import com.krishagni.openspecimen.msk2.events.CarsBiospecimenDetail;

public interface CarsBiospecimenReader extends Closeable {
	CarsBiospecimenDetail next();
}
