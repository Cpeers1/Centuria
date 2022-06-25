package org.asf.emuferal.entities.components.generic;

import org.asf.emuferal.entities.components.Component;
import org.asf.emuferal.entities.components.InventoryItemComponent;

import com.google.gson.JsonObject;

/**
 * Generic component for giving an item a custom name.
 * @author Owenvii
 *
 */
@Component
public class NameComponent extends InventoryItemComponent {

	public static final String COMPONENT_NAME = "Name";
	
	public static final String NAME_PROPERTY_NAME = "name";
	
	public String name = "";
	
	@Override
	public String getComponentName() {
		return COMPONENT_NAME;
	}

	@Override
	public JsonObject toJson() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(NAME_PROPERTY_NAME, name);
		return jsonObject;
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		this.name = object.get(NAME_PROPERTY_NAME).getAsString();
	}

}
