package org.asf.centuria.entities.components.uservars;

import java.util.ArrayList;
import java.util.List;
import org.asf.centuria.entities.components.InventoryItemComponent;
import org.asf.centuria.entities.uservars.UserVarValue;
import org.asf.centuria.enums.uservars.UserVarType;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Base class for all UserVarComponents.
 * 
 * @author Owenvii
 */
public abstract class UserVarComponent extends InventoryItemComponent {

	public static final String VALUES_PROPERTY_NAME = "values";

	private List<Integer> values = new ArrayList<Integer>();

	/**
	 * Returns the type of the user var, based on its component name.
	 * 
	 * @return The user var type.
	 */
	public UserVarType getUserVarType() {
		for (var type : UserVarType.values()) {
			if (type.componentName == this.getComponentName()) {
				return type;
			}
		}

		return null;
	}

	@Override
	public JsonObject toJson() {
		JsonObject newComponent = new JsonObject();
		JsonObject newValueObject = new JsonObject();

		int index = 0;
		for (var entry : values) {
			newValueObject.addProperty(Integer.toString(index), entry);
			index++;
		}

		newComponent.addProperty(VALUES_PROPERTY_NAME, newValueObject.toString());

		return newComponent;
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		String valuesJsonString = object.get(VALUES_PROPERTY_NAME).getAsString();

		// Parse the values json string
		var valuesObject = JsonParser.parseString(valuesJsonString).getAsJsonObject();

		for (var valueObject : valuesObject.entrySet()) {
			while (values.size() <= Integer.parseInt(valueObject.getKey()))
				values.add(0);
			values.set(Integer.parseInt(valueObject.getKey()), valueObject.getValue().getAsInt());
		}
	}

	public UserVarValue getUserVarValue(int index) {
		var value = new UserVarValue();
		value.index = index;
		value.value = values.get(index);

		return value;
	}

	public UserVarValue[] getAllUserVarValues() {
		UserVarValue[] userVarValues = new UserVarValue[values.size()];

		int index = 0;
		for (var entry : values) {
			var newUserVarValue = new UserVarValue();
			newUserVarValue.index = index;
			newUserVarValue.value = entry;
			userVarValues[index] = newUserVarValue;
			index++;
		}

		return userVarValues;
	}

	public void setUserVarValue(UserVarValue valueToSet) {
		if (values.size() <= valueToSet.index) {
			values.add(valueToSet.value);
		} else {
			values.set(valueToSet.index, valueToSet.value);
		}

	}

	public void deleteUserVarValue(int index) {
		values.remove(index);
	}

}
