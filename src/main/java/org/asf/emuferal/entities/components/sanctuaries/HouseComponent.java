package org.asf.emuferal.entities.components.sanctuaries;

import org.asf.emuferal.entities.components.Component;
import org.asf.emuferal.entities.components.InventoryItemComponent;

import com.google.gson.JsonObject;

/***
 * Sanctuary house component.
 * Used to store data about the house object.
 * @author Owenvii
 *
 */
@Component
public class HouseComponent extends InventoryItemComponent {

	public static final String COMPONENT_NAME = "House";

	public static final String STAGE_PROPERTY_NAME = "stage";
	public static final String ENLARGED_AREAS_PROPERTY_NAME = "enlargedAreas";

	public int stage;
	public int[] enlargedAreas = new int[10];

	
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