package com.krishagni.openspecimen.sgh.services;

import java.io.IOException;
import java.util.List;

public interface OutputCsvFileService {
	public void writeNext(List<Object> data);
	
	public void closeWriter();
	
	public void flush() throws IOException;

	public void initCsvWriter(String file) throws IOException;
	
	public void incrementResultSize();

	public Integer getResultSize();
}
