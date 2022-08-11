package org.asf.centuria.entities.sanctuaries;

import org.asf.centuria.entities.JsonableObject;
import org.asf.centuria.entities.objects.WorldObjectPositionInfo;

import com.google.gson.JsonObject;

/**
 * This class is used by {@link PlacementInfo} to define the properties of placed objects in a sanctuary.
 * @author Owenvii
 *
 */
public class PlacedItemInfo extends JsonableObject {

	public static final String POS_X_PROPERTY_NAME = "xPos";
	public static final String POS_Y_PROPERTY_NAME = "yPos";
	public static final String POS_Z_PROPERTY_NAME = "zPos";
	
	public static final String ROT_X_PROPERTY_NAME = "rotX";
	public static final String ROT_Y_PROPERTY_NAME = "rotY";
	public static final String ROT_Z_PROPERTY_NAME = "rotZ";
	public static final String ROT_W_PROPERTY_NAME = "rotW";
	
	public static final String GRID_ID_PROPERTY_NAME = "gridID";
	public static final String PARENT_ID_PROPERTY_NAME = "parentItemId";
	public static final String PLACEABLE_INV_ID_PROPERTY_NAME = "placeableInvId";
	public static final String STATE_PROPERTY_NAME = "state";
	
	public WorldObjectPositionInfo worldObjectPositionInfo = new WorldObjectPositionInfo();
	public int gridId = 0;
	public String parentId = "";
	public String placeableInvId = "";
	public int state = 0;
	
	@Override
	public JsonObject toJson() {
		JsonObject object = new JsonObject();
		
		// Position
		object.addProperty(POS_X_PROPERTY_NAME, worldObjectPositionInfo.position.x);
		object.addProperty(POS_Y_PROPERTY_NAME, worldObjectPositionInfo.position.y);
		object.addProperty(POS_Z_PROPERTY_NAME, worldObjectPositionInfo.position.z);
		
		// Rotation
		object.addProperty(ROT_X_PROPERTY_NAME, worldObjectPositionInfo.rotation.x);
		object.addProperty(ROT_Y_PROPERTY_NAME, worldObjectPositionInfo.rotation.y);
		object.addProperty(ROT_Z_PROPERTY_NAME, worldObjectPositionInfo.rotation.z);
		object.addProperty(ROT_W_PROPERTY_NAME, worldObjectPositionInfo.rotation.w);
		
		// Other Data
		object.addProperty(GRID_ID_PROPERTY_NAME, gridId);
		object.addProperty(PARENT_ID_PROPERTY_NAME, parentId);
		object.addProperty(PLACEABLE_INV_ID_PROPERTY_NAME, placeableInvId);
		object.addProperty(STATE_PROPERTY_NAME, state);
		
		return object;
	}

	@Override
	protected void propagatePropertiesFromJson(JsonObject jsonObject) {
		worldObjectPositionInfo = new WorldObjectPositionInfo(
				// Position
				jsonObject.get(POS_X_PROPERTY_NAME).getAsDouble(),
				jsonObject.get(POS_Y_PROPERTY_NAME).getAsDouble(),
				jsonObject.get(POS_Z_PROPERTY_NAME).getAsDouble(),
				
				// Rotation
				jsonObject.get(ROT_X_PROPERTY_NAME).getAsDouble(),
				jsonObject.get(ROT_Y_PROPERTY_NAME).getAsDouble(),
				jsonObject.get(ROT_Z_PROPERTY_NAME).getAsDouble(),
				jsonObject.get(ROT_W_PROPERTY_NAME).getAsDouble()
				);
		
		// Other Data
		gridId = jsonObject.get(GRID_ID_PROPERTY_NAME).getAsInt();
		parentId = jsonObject.get(PARENT_ID_PROPERTY_NAME).getAsString();
		placeableInvId = jsonObject.get(PLACEABLE_INV_ID_PROPERTY_NAME).getAsString();
		state = jsonObject.get(STATE_PROPERTY_NAME).getAsInt();
	}

}
