package org.asf.emuferal.entities.components;

import java.lang.reflect.InvocationTargetException;

import com.google.gson.JsonObject;

/**
 * Base class for all inventory item components.
 * 
 * @author Owenvii
 *
 */
public abstract class InventoryItemComponent {

	/**
	 * Gets the component name. This should be the name the object is stored as
	 * under the components part of the inventory item.
	 * 
	 * @return The component's name.
	 */
	public abstract String getComponentName();

	/**
	 * Converts this component to a JsonObject.
	 * 
	 * @return The JsonObject component.
	 */
	public abstract JsonObject toJson();

	/**
	 * Populates the component with values from the jsonObject.
	 */
	protected abstract void getPropertiesFromJson(JsonObject object);

	/**
	 * Populates a new component of type T with properties from a JsonObject.
	 * 
	 * @param <T>        The type of the component.
	 * @param type       The type of the component.
	 * @param jsonObject The object to retrieve properties from.
	 * @return A new inventory item component of type T.
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static <T extends InventoryItemComponent> InventoryItemComponent fromJson(Class<T> type,
			JsonObject jsonObject) throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		// create a new component of type t
		var newComponent = type.getDeclaredConstructor().newInstance();

		// populate it with properties from the json object.
		newComponent.getPropertiesFromJson(jsonObject);

		// return it
		return newComponent;
	}

}
