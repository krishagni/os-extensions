package com.krishagni.openspecimen.redcap.crf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import edu.common.dynamicextensions.domain.nui.ComboBox;
import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.DataType;
import edu.common.dynamicextensions.domain.nui.DatePicker;
import edu.common.dynamicextensions.domain.nui.Label;
import edu.common.dynamicextensions.domain.nui.MultiSelectCheckBox;
import edu.common.dynamicextensions.domain.nui.NumberField;
import edu.common.dynamicextensions.domain.nui.PermissibleValue;
import edu.common.dynamicextensions.domain.nui.PvDataSource;
import edu.common.dynamicextensions.domain.nui.PvVersion;
import edu.common.dynamicextensions.domain.nui.RadioButton;
import edu.common.dynamicextensions.domain.nui.SelectControl;
import edu.common.dynamicextensions.domain.nui.StringTextField;

public class Field {	
	private Map<String, String> attrs = new HashMap<String, String>();
	
	public Field(Map<String, String> attrs) { 
		this.attrs = attrs;		
	}
	
	public boolean hasSectionHeader() {
		return StringUtils.isNotBlank(attrs.get("section_header"));
	}
	
	public Control getSectionHeader() {
		Label label = (Label)new LabelFieldFactory().getControl();
		label.setHeading(true);
		label.setName(label.getName() + "_heading");
		label.setUserDefinedName(label.getUserDefinedName() + "_heading");
		label.setCaption(attrs.get("section_header"));
		return label;
	}
				
	public Control getDeField() {
		return new ControlBuilder().getControl();
	}
		
	private class ControlBuilder {
		public Control getControl() {
			String fieldType = attrs.get("field_type");
			String validationType = attrs.get("text_validation_type_or_show_slider_number");
			
			ControlFactory fieldFactory = null;
			if (fieldType.equals("calc") || validationType.equals("number") || validationType.equals("integer")) {
				fieldFactory = new NumberFieldFactory();
			} else if (validationType.startsWith("date_")) {
				fieldFactory = new DateFieldFactory();
			} else if (fieldType.equals("radio")) {
				fieldFactory = new RadioButtonFieldFactory();
			} else if (fieldType.equals("checkbox")) {
				fieldFactory = new CheckboxFieldFactory();
			} else if (fieldType.equals("dropdown")) {
				fieldFactory = new DropdownFieldFactory(); 
			} else if (fieldType.equals("descriptive")) {
				fieldFactory = new LabelFieldFactory();
			} else if (fieldType.equals("text")) {
				fieldFactory = new TextFieldFactory();
			} else {
				throw new IllegalArgumentException("Invalid field type: " + fieldType);
			}
			
			return fieldFactory.getControl();			
		}		
	}
	
	private abstract class ControlFactory {
		public abstract Control getControl();
		
		protected void setAttrs(Control ctrl) {
			setName(ctrl);
			setCaption(ctrl);
			setIdentified(ctrl);
			setRequired(ctrl);			
		}
		
		protected Boolean parseBoolean(String value) {
			return StringUtils.isNotBlank(value) && Boolean.parseBoolean(value);
		}
		
		protected int parseInt(String value, int defValue) {
			if (StringUtils.isBlank(value)) {
				return defValue;
			}
			
			return Integer.parseInt(value);
		}
				
		private void setName(Control ctrl) {
			String fieldName = attrs.get("field_name");
			if (StringUtils.isBlank(fieldName)) {
				throw new IllegalArgumentException("Field name can not be blank");
			}
			
			ctrl.setName(fieldName);
			ctrl.setUserDefinedName(fieldName);
		}
		
		private void setCaption(Control ctrl) {
			String caption = attrs.get("field_label");
			if (StringUtils.isBlank(caption)) {
				throw new IllegalArgumentException("Field display name or caption can not be blank");
			}
			
			ctrl.setCaption(caption);
		}
		
		private void setIdentified(Control ctrl) {
			ctrl.setPhi(parseBoolean(attrs.get("identifier")));
		}
		
		private void setRequired(Control ctrl) {
			ctrl.setMandatory(parseBoolean(attrs.get("required")));
		}		
	}
	
	private class TextFieldFactory extends ControlFactory {
		@Override
		public Control getControl() {
			StringTextField textField = new StringTextField();			
			setAttrs(textField);			
			textField.setMinLength(parseInt(attrs.get("text_validation_min"), 0));
			textField.setMaxLength(parseInt(attrs.get("text_validation_max"), 0));
			return textField;
		}		
	}
	
	private class NumberFieldFactory extends ControlFactory {
		@Override
		public Control getControl() {
			NumberField numberField = new NumberField();			
			setAttrs(numberField);
			
			String fieldType = attrs.get("field_type");
			if (fieldType.equals("calc") || fieldType.equals("number")) {
				numberField.setNoOfDigitsAfterDecimal(10);
			}
			
			return numberField;
		}		
	}
	
	private class DateFieldFactory extends ControlFactory {
		@Override
		public Control getControl() {
			DatePicker dateField = new DatePicker();
			setAttrs(dateField);
			
			String validationType = attrs.get("text_validation_type_or_show_slider_number");
			String fmt = validationType.substring(validationType.indexOf("_") + 1);
			if (fmt.equals("mdy")) {
				dateField.setFormat("MM-dd-yyyy");
			} else {
				dateField.setFormat("dd-MM-yyyy");				
			}
			
			return dateField;
		}		
	}
	
	private abstract class SelectFieldFactory extends ControlFactory {
		public void setAttrs(SelectControl ctrl) {
			super.setAttrs(ctrl);
			ctrl.setPvDataSource(getPvDataSource(attrs));
		}
				
		private PvDataSource getPvDataSource(Map<String, String> attrs) {
			PvVersion pvVersion = new PvVersion();
			pvVersion.setPermissibleValues(getPvs(attrs.get("select_choices_or_calculations")));
			
			PvDataSource pvDataSource = new PvDataSource();
			pvDataSource.setDataType(DataType.STRING);
			pvDataSource.getPvVersions().add(pvVersion);
			return pvDataSource;			
		}
		
		private List<PermissibleValue> getPvs(String choicesStr) {
			List<PermissibleValue> pvs = new ArrayList<PermissibleValue>();
			for (String choice : choicesStr.split("\\|")) {
				String[] keyValue = choice.split(",");
				
				PermissibleValue pv = new PermissibleValue();
				pv.setOptionName(keyValue[0].trim());
				pv.setValue(keyValue[1].trim());				
				pvs.add(pv);
			}
			
			return pvs;
		}
	}
	
	private class DropdownFieldFactory extends SelectFieldFactory {
		@Override
		public Control getControl() {
			ComboBox dropdownField = new ComboBox();
			setAttrs(dropdownField);
			return dropdownField;
		}		
	}
	
	private class RadioButtonFieldFactory extends SelectFieldFactory {
		@Override
		public Control getControl() {
			RadioButton radioBtnField = new RadioButton();
			setAttrs(radioBtnField);			
			return radioBtnField;
		}		
	}
	
	private class CheckboxFieldFactory extends SelectFieldFactory {
		@Override
		public Control getControl() {
			MultiSelectCheckBox checkboxField = new MultiSelectCheckBox();
			setAttrs(checkboxField);
			return checkboxField;
		}		
	}
	
	private class LabelFieldFactory extends ControlFactory {
		@Override
		public Control getControl() {
			Label label = new Label();
			setAttrs(label);
			label.setNote(true);
			return label;
		}		
	}
}