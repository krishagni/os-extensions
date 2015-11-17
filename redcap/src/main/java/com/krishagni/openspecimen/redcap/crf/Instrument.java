package com.krishagni.openspecimen.redcap.crf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.krishagni.openspecimen.redcap.Project;

import edu.common.dynamicextensions.domain.nui.Container;

public class Instrument {
	private boolean loaded;
	
	private String name;
	
	private String caption;
	
	private Project project;
	
	private List<Field> fields = new ArrayList<Field>();

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

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public List<Field> getFields() {
		return fields;
	}

	public void setFields(List<Field> fields) {
		this.fields = fields;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj == null) {
			return false;
		}
		
		if (getClass() != obj.getClass()) {
			return false;
		}
		
		Instrument other = (Instrument) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		
		return true;
	}

	@SuppressWarnings("unchecked")
	public void loadInstrument() {
		if (loaded) {
			return;
		}
		
		MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
		form.add("token", project.getApiToken());
		form.add("content", "metadata");
		form.add("forms[0]", getName());
		form.add("format", "json");
		
		Map<String, String>[] fieldDataMap = new RestTemplate().postForObject(project.getApiUrl(), form, Map[].class);
		
		List<Field> fields = new ArrayList<Field>();
		for (Map<String, String> fieldData : fieldDataMap) {
			fields.add(new Field(fieldData));
		}
		
		setFields(fields);
		loaded = true;
	}
	
	public Container getDeForm() {
		if (StringUtils.isBlank(name)) {
			throw new IllegalStateException("Form name can not be blank");
		}
		
		if (StringUtils.isBlank(caption)) {
			throw new IllegalStateException("Form caption/display name can not be blank");
		}
		
		Container form = new Container();
		form.setName(getName());
		form.setCaption(getCaption());
		form.useAsDto();
		
		for (Field field : getFields()) {
			if (field.hasSectionHeader()) {
				form.addControl(field.getSectionHeader());
			}
			
			form.addControl(field.getDeField());			
		}
		
		return form;
	}
}