package org.asf.emuferal.entities.components.sanctuaries;

import org.asf.emuferal.entities.components.Component;
import org.asf.emuferal.entities.components.InventoryItemComponent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Component that stores data for a sanctuary class.
 * 
 * @author Owenvii
 *
 */
@Component
public class SanctuaryClassComponent extends InventoryItemComponent {
	public static final String COMPONENT_NAME = "SanctuaryLook";

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
		var object = new JsonObject();
		object.addProperty(STAGE_PROPERTY_NAME, stage);
		var jsonArray = new JsonArray();

		for (var item : enlargedAreas)
			jsonArray.add(item);

		object.add(ENLARGED_AREAS_PROPERTY_NAME, jsonArray);
		return object;
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		stage = object.get(STAGE_PROPERTY_NAME).getAsInt();

		var enlargedAreaJsonArray = object.get(ENLARGED_AREAS_PROPERTY_NAME).getAsJsonArray();

		for (int i = 0; i < enlargedAreaJsonArray.size(); i++) {
			enlargedAreas[i] = enlargedAreaJsonArray.get(i).getAsInt();
		}
	}
}
