package org.asf.emuferal.entities.sanctuaries;

import java.util.ArrayList;
import java.util.List;

import org.asf.emuferal.entities.JsonableObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * This class is used by {@link SanctuaryInfo} to define placeable objects in the sanctuary.
 * @author Owenvii
 *
 */
public class PlacementInfo extends JsonableObject {

	public static final String ITEMS_PROPERTY_NAME = "items";
	public List<PlacedItemInfo> items = new ArrayList<PlacedItemInfo>();
	
	@Override
	public JsonObject toJson() {
		JsonObject jsonObject = new JsonObject();
		JsonArray itemsArray = new JsonArray();
		
		for(var item : items)
			itemsArray.add(item.toJson());
			
		jsonObject.add(ITEMS_PROPERTY_NAME, itemsArray);
		
		return jsonObject;
	}

	@Override
	protected void propagatePropertiesFromJson(JsonObject jsonObject) {
		JsonArray itemsArray = jsonObject.get(ITEMS_PROPERTY_NAME).getAsJsonArray();
		
		for(var item : itemsArray)
		{
			try {
				items.add((PlacedItemInfo)new PlacedItemInfo().CreateObjectFromJson(item.getAsJsonObject()));
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}
}
