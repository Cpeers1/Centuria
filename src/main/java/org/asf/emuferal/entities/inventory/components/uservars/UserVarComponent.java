package org.asf.emuferal.entities.inventory.components.uservars;

import java.util.HashMap;
import java.util.Map;

import org.asf.emuferal.entities.inventory.components.InventoryItemComponent;
import org.asf.emuferal.entities.inventory.uservars.UserVarValue;
import org.asf.emuferal.enums.inventory.uservars.UserVarType;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Base class for all UserVarComponents.
 * 
 * @author Owenvii
 */
public abstract class UserVarComponent extends InventoryItemComponent {

	private static String valuePropertyName = "values";

	private Map<Integer, Integer> values = new HashMap<Integer, Integer>();

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
		// TODO Auto-generated method stub

		JsonObject newComponent = new JsonObject();
		JsonObject newValueObject = new JsonObject();

		for (var entry : values.entrySet()) {
			newValueObject.addProperty(entry.getKey().toString(), entry.getValue());
		}

		newComponent.addProperty(valuePropertyName, newValueObject.toString());

		return newComponent;
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		String valuesJsonString = object.get(valuePropertyName).getAsString();

		// Parse the values json string

		var valuesObject = JsonParser.parseString(valuesJsonString).getAsJsonObject();

		for (var valueObject : valuesObject.entrySet()) {
			values.put(Integer.parseInt(valueObject.getKey()), valueObject.getValue().getAsInt());
		}
	}

	/**
	 * Gets the value from a userVar, given its index. Low level method to be used
	 * by an accessor.
	 * 
	 * @param index The index of the value you want to get.
	 * @return The user var's value item.
	 */
	public UserVarValue getUserVarValue(int index) {
		var value = new UserVarValue();
		value.index = index;
		value.value = values.get(index);

		return value;
	}

	/**
	 * Sets a value from a userVar, given its index. Low level method to be used by
	 * an accessor.
	 * 
	 * @param index The index of the value you want to set.
	 * @return The result item, containing
	 */
	public void setUserVarValue(UserVarValue valueToSet) {
		values.put(valueToSet.index, valueToSet.value);
	}

	/**
	 * Deletes a value from a userVar, given its index. Low level method to be used
	 * by an accessor.
	 * 
	 * @param index The index of the value you want to delete.
	 */
	public void deleteUserVarValue(int index) {
		values.remove(index);
	}

}
