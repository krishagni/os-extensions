package com.krishagni.openspecimen.redcap.domain;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class LogEvent {
	public enum Type {
		INSERT,
		UPDATE,
		DELETE,
		MANAGE,
		DATA_EXPORT
	};
	
	private String pid;
	
	private Date ts;
	
	private Type type;
	
	private String pk;
	
	private Map<String, String> dataValues;

	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public Date getTs() {
		return ts;
	}

	public void setTs(Date ts) {
		this.ts = ts;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getPk() {
		return pk;
	}

	public void setPk(String pk) {
		this.pk = pk;
	}

	public Map<String, String> getDataValues() {
		return dataValues;
	}

	public void setDataValues(Map<String, String> dataValues) {
		this.dataValues = dataValues;
	}
	
	public boolean isInsert() {
		return type == Type.INSERT;
	}
	
	public boolean isUpdate() {
		return type == Type.UPDATE;
	}
	
	public boolean isDelete() {
		return type == Type.DELETE;
	}
	
	public String toString() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(this);			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public static LogEvent parse(Map<String, String> eventValues) {		
		LogEvent event = new LogEvent();
		event.setPid(eventValues.get(PID));
		
		String tsStr = eventValues.get(TS);
		try {			
			Date ts = new SimpleDateFormat(TS_PATTERN).parse(tsStr);
			event.setTs(ts);			
		} catch (Exception e) {
			throw new RuntimeException("Error parsing timestamp: " + tsStr);
		}
		
		event.setType(Type.valueOf(eventValues.get(EVENT)));
		event.setPk(eventValues.get(PK));
		
		String dataValueStr = eventValues.get(DATA_VALUES);
		String[] props = dataValueStr.split(",\n");
		
		Map<String, String> dataValues = new HashMap<String, String>();
		for (String prop : props) {
			String[] kv = prop.split("=");
			dataValues.put(kv[0].trim(), kv[1].trim().substring(1, kv[1].trim().length() - 1));
		}
		
		event.setDataValues(dataValues);		
		return event;
	}
		
	private static final String PID = "project_id";
	
	private static final String TS = "ts";
	
	private static final String EVENT = "event";
	
	private static final String PK = "pk";
	
	private static final String DATA_VALUES = "data_values";
	
	private static final String TS_PATTERN = "yyyyMMddhhmmss";
}
