package org.asf.emuferal.entities.sanctuaries;

import org.asf.emuferal.entities.JsonableObject;

import com.google.gson.JsonObject;

/**
 * Sanctuary Info Object.
 * Used to store info about the sanctuary.
 * Gets populated into a {@link SanctuaryInfo} component.
 * @author Owenvii
 *
 */
public class SanctuaryInfo extends JsonableObject {

	public static String houseDefId_PropertyName = "houseDefId";
	public static String houseInvId_PropertyName = "houseInvId";
	public static String islandDefId_PropertyName = "islandDefId";
	public static String islandInvId_PropertyName = "islandInvId";
	public static String classInvId_PropertyName = "classInvId";
	public static String placementInfo_PropertyName = "placementInfo";
	
	public int houseDefID;
	public String houseInvId;
	public int islandDefId;
	public String islandInvId;
	public String classInvId;
	public PlacementInfo placementInfo;
	
	@Override
	public JsonObject toJson() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected JsonObject propagatePropertiesFromJson(JsonObject jsonObject) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
}
