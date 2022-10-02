package org.asf.centuria.entities.components.sanctuaries;

import org.asf.centuria.entities.components.Component;
import org.asf.centuria.entities.components.InventoryItemComponent;

import com.google.gson.JsonArray;
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
		
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(STAGE_PROPERTY_NAME, stage);
		var array = new JsonArray();
		for(var item : enlargedAreas)
		{
			array.add(item);
		}
		
		jsonObject.add(ENLARGED_AREAS_PROPERTY_NAME, array);
		
		return jsonObject;
	}

	@Override
	protected void getPropertiesFromJson(JsonObject object) {
		stage = object.get(STAGE_PROPERTY_NAME).getAsInt();
		var items = object.get(ENLARGED_AREAS_PROPERTY_NAME).getAsJsonArray();
		
		for(int i = 0; i > 10; i++)
		{
			enlargedAreas[i] = items.get(i).getAsInt();
		}
	}

}