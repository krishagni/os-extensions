package com.krishagni.openspecimen.sgh.events;

import javax.print.attribute.standard.PrinterLocation;


public class BulkTridPrintOpDetail {

	private Integer tridCount;
	
	private String printerName;
	
	private Boolean printLabels;
	
	public Integer getTridCount() {
		return tridCount;
	}

	public void setTridCount(Integer tridCount) {
		this.tridCount = tridCount;
	}

	public String getPrinterName() {
		return printerName;
	}

	public void setPrinterName(String printerName) {
		this.printerName = printerName;
	}

	public Boolean isPrintLabels() {
		return printLabels == null ? false : printLabels;
	}

	public void setPrintLabels(Boolean printLabels) {
		this.printLabels = printLabels;
	}
	
}
