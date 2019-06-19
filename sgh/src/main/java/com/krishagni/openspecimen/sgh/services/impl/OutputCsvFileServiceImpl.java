package com.krishagni.openspecimen.sgh.services.impl;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.krishagni.openspecimen.sgh.services.OutputCsvFileService;

import au.com.bytecode.opencsv.CSVWriter;

public class OutputCsvFileServiceImpl implements OutputCsvFileService {
	private CSVWriter csvWriter;
	
	private Integer resultSize;
	
	@Override
	public void writeNext(List<Object> data) {
		csvWriter.writeNext(data.toArray(new String[0]));
	}

	@Override
	public void closeWriter() {
		IOUtils.closeQuietly(csvWriter);
	}
	
	@Override
	public void initCsvWriter(String file) throws IOException {
		this.resultSize = 0;
		csvWriter = new CSVWriter(new FileWriter(file, true));
	}

	@Override
	public Integer getResultSize() {
		return resultSize;
	}

	@Override
	public void incrementResultSize() {
		this.resultSize++;
	}

	@Override
	public void flush() throws IOException {
		csvWriter.flush();
	}
}