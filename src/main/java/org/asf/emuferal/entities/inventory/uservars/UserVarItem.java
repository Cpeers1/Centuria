package org.asf.emuferal.entities.inventory.uservars;

import org.asf.emuferal.entities.inventory.InventoryItem;
import org.asf.emuferal.enums.inventory.uservars.UserVarType;

import com.google.gson.JsonObject;

public class UserVarItem extends InventoryItem {
	
	public static int InvType = 303;
	
	public UserVarValue[] values;
	public UserVarType userVarType;
	
	public UserVarItem(UserVarValue[] values, UserVarType userVarType, int defId, String uuid)
	{
		super(defId, uuid, InvType);
		this.values = values;
		this.userVarType = userVarType;
	}
	
	public UserVarItem(int defId, String uuid, int invType)
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
