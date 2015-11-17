package com.krishagni.openspecimen.redcap.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.krishagni.openspecimen.redcap.crf.Instrument;

public class InstrumentDetail {
	private String name;
	
	private String caption;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}
	
	public static InstrumentDetail from(Instrument instrument) { 
		InstrumentDetail result = new InstrumentDetail();
		result.setName(instrument.getName());
		result.setCaption(instrument.getCaption());
		return result;
	}
	
	public static List<InstrumentDetail> from(Collection<Instrument> instruments) {
		List<InstrumentDetail> result = new ArrayList<InstrumentDetail>();
		for (Instrument instrument : instruments) {
			result.add(from(instrument));
		}
		
		return result;
	}
}
