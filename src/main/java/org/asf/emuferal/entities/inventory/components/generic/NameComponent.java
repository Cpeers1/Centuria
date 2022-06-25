package org.asf.emuferal.entities.inventory.components.generic;

import org.asf.emuferal.entities.inventory.components.Component;
import org.asf.emuferal.entities.inventory.components.InventoryItemComponent;

import com.google.gson.JsonObject;

/**
 * Generic component for giving an item a custom name.
 * @author Owenvii
 *
 */
@Component
public class NameComponent extends InventoryItemComponent {

	public static String ComponentName = "Name";
	
	public static String name_PropertyName = "name";
	
	public String name = "";
	
	@Override
	public String getComponentName() {
		return ComponentName;
	}

	@Override
	public JsonObject toJson() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(name_PropertyName, name);
		return jsonObject;
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		this.name = object.get(name_PropertyName).getAsString();
	}

}
