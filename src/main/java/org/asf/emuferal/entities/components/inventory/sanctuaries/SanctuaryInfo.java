package org.asf.emuferal.entities.components.inventory.sanctuaries;

/**
 * Sanctuary Info Object.
 * Used to store info about the sanctuary.
 * Gets populated into a {@link SanctuaryInfo} component.
 * @author Owenvii
 *
 */
public class SanctuaryInfo {

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
	
	
	
}
