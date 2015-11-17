package com.krishagni.openspecimen.redcap;

import com.krishagni.openspecimen.redcap.domain.LogEvent;

public interface FieldTransformer {
	public LogEvent transform(LogEvent input);
}
