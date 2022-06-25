package org.asf.emuferal.entities.inventory.playervars;

import org.asf.emuferal.entities.inventory.InventoryItem;
import org.asf.emuferal.enums.inventory.UserVarType;

import com.google.gson.JsonObject;

public class PlayerVarItem extends InventoryItem {
	
	public static int InvType = 303;
	
	public PlayerVarValue[] values;
	public UserVarType userVarType;
	
	public PlayerVarItem(PlayerVarValue[] values, UserVarType userVarType, int defId, String uuid)
	{
		super(defId, uuid, InvType);
		this.values = values;
		this.userVarType = userVarType;
	}
	
	public PlayerVarItem(int defId, String uuid, int invType)
	{
		super(defId, uuid, InvType);
	}	
	
	public JsonObject toJsonObject()
	{
		JsonObject object = new JsonObject();
		
		//populate the new object with specific components
		
		return object;
	}
}
