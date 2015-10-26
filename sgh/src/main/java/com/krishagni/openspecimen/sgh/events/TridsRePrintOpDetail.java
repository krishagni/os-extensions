package com.krishagni.openspecimen.sgh.events;

import java.util.List;


public class TridsRePrintOpDetail {

	private List<String> trids;
	
	private String printerName;
	
	public List<String> getTrids() {
		return trids;
	}

	public void setTrids(List<String> trids) {
		this.trids = trids;
	}
	
	public String getPrinterName() {
		return printerName;
	}

	public void setPrinterName(String printerName) {
		this.printerName = printerName;
	}
	
}
