package org.asf.centuria.entities.components.generic;

import org.asf.centuria.entities.components.Component;
import org.asf.centuria.entities.components.InventoryItemComponent;

import com.google.gson.JsonObject;

@Component
public class PlaceableComponent extends InventoryItemComponent {

	public static final String COMPONENT_NAME = "Placeable";

	@Override
	public String getComponentName() {
		return COMPONENT_NAME;
	}

	@Override
	public JsonObject toJson() {
		return new JsonObject();
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		// Absolutely nothing to do here...
	}

}
