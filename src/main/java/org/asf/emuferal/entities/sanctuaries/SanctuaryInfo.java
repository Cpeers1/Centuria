package org.asf.emuferal.entities.sanctuaries;

import org.asf.emuferal.entities.JsonableObject;

import com.google.gson.JsonObject;

/**
 * Sanctuary Info Object.
 * Used to store info about the sanctuary.
 * Gets populated into a {@link SanctuaryLookComponent} component.
 * @author Owenvii
 *
 */
public class SanctuaryInfo extends JsonableObject {

	public static final String HOUSE_DEF_ID_PROPERTY_NAME = "houseDefId";
	public static final String HOUSE_INV_ID_PROPERTY_NAME = "houseInvId";
	public static final String ISLAND_DEF_ID_PROPERTY_NAME = "islandDefId";
	public static final String ISLAND_INV_ID_PROPERTY_NAME = "islandInvId";
	public static final String CLASS_INV_ID_PROPERTY_NAME = "classInvId";
	public static final String PLACEMENT_INFO_PROPERTY_NAME = "placementInfo";
	
	public int houseDefID;
	public String houseInvId;
	public int islandDefId;
	public String islandInvId;
	public String classInvId;
	public PlacementInfo placementInfo;
	
	@Override
	public JsonObject toJson() {
		JsonObject object = new JsonObject();
		object.addProperty(HOUSE_DEF_ID_PROPERTY_NAME, houseDefID);
		object.addProperty(HOUSE_INV_ID_PROPERTY_NAME, houseInvId);
		object.addProperty(ISLAND_DEF_ID_PROPERTY_NAME, islandDefId);
		object.addProperty(ISLAND_INV_ID_PROPERTY_NAME, islandInvId);
		object.addProperty(CLASS_INV_ID_PROPERTY_NAME, classInvId);
		object.add(PLACEMENT_INFO_PROPERTY_NAME, placementInfo.toJson());
		return object;
	}
	
	@Override
	protected void propagatePropertiesFromJson(JsonObject jsonObject) {
		this.houseDefID = jsonObject.get(HOUSE_DEF_ID_PROPERTY_NAME).getAsInt();
		this.houseInvId = jsonObject.get(HOUSE_INV_ID_PROPERTY_NAME).getAsString();
		this.islandDefId = jsonObject.get(ISLAND_DEF_ID_PROPERTY_NAME).getAsInt();
		this.houseInvId = jsonObject.get(ISLAND_INV_ID_PROPERTY_NAME).getAsString();
		this.classInvId = jsonObject.get(CLASS_INV_ID_PROPERTY_NAME).getAsString();
		
		try {
			this.placementInfo = (PlacementInfo)new PlacementInfo().CreateObjectFromJson(jsonObject.get(PLACEMENT_INFO_PROPERTY_NAME).getAsJsonObject());
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	
	
}
