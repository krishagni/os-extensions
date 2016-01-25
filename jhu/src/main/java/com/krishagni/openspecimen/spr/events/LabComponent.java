package com.krishagni.openspecimen.spr.events;

/**
 * <h1>This class contains the fields of a lab component.</h1>
 * @author Bob Lange
 * @since 2015-05-30
 *
 */
public class LabComponent {
	private String componentName;
	private String componentMnemonic;
	private String componentCode;
	private String componentSubId;
	private String result;
	private String units;
	private String range;
	private String observResultStatus;
	private String flags;
	private String componentNote;
	
	public String getComponentName() {
		return componentName;
	}
	public String getComponentMnemonic() {
		return componentMnemonic;
	}
	public String getComponentCode() {
		return componentCode;
	}
	public String getComponentSubId() {
		return componentSubId;
	}
	public String getResult() {
		return result;
	}
	public String getUnits() {
		return units;
	}
	public String getRange() {
		return range;
	}
	public String getObservResultStatus() {
		return observResultStatus;
	}
	public String getFlags() {
		return flags;
	}
	public String getComponentNote() {
		return componentNote;
	}
}
