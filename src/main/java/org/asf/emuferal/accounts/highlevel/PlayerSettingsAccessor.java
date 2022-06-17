package org.asf.emuferal.accounts.highlevel;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.UUID;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class PlayerSettingsAccessor {
	
	public enum UserVarType
	{
		Any(0, "UserVarCustom"),
		Counter(1, "UserVarCounter"), //TODO: Confirm name of this.
		Highest(2, "UserVarHighest"),
		Lowest(3, "UserVarLowest"), //TODO: Confirm name of this.
		Bit(4, "UserVarBit"), //TODO: Confirm name of this.
		BitOnOnly(5, "UserVarBitOnOnly"); //TODO: Confirm name of this.
		
		public int val;
		public String componentName;
		
		UserVarType(int val, String componentName)
		{
			this.val = val;
			this.componentName = componentName;
		}	
	}

	private PlayerInventory inventory;

	public PlayerSettingsAccessor(PlayerInventory inventory) {
		this.inventory = inventory;
	}
	
	public boolean setPlayerVars(int defId, int[] values)
	{
		try {
			
			if (!inventory.getAccessor().hasInventoryObject("303", defId))
			{
				//create the inventory object
				JsonObject newVarObject = createNewPlayerVars(defId, values);
				
				var inv = inventory.getItem("303");
				//UHH
				inv.getAsJsonArray().add(newVarObject);
				
				inventory.setItem("303", inv);
				return true;
			}
			else
			{
				var inv = inventory.getItem("303");
				
				//find the item
				
				JsonElement element = null;
				
				for(var item : inv.getAsJsonArray())
				{
					if(item.getAsJsonObject().get("defId").getAsInt() == defId)
					{
						element = item;
						break;
					}
				}
				
				if(element == null) return false; //cannot find
				
				//go through the levels of bullshit
				
				String typeName = getVarType(defId).componentName;
				
				var sortThrough = element.getAsJsonObject().get("components").getAsJsonObject().get(typeName).getAsJsonObject();
				
				sortThrough.remove("values");
				
				sortThrough.add("values", element);
				
			    int index = 0;
			    JsonObject varArray = new JsonObject();
			    
			    for(int value : values)
			    {
			    	varArray.addProperty(String.valueOf(index), value);
			    	index++;
			    }
			    
			    //convert this to a string object and store it in values
			    sortThrough.addProperty("values", varArray.toString());
			    
			    inventory.setItem("303", inv);
				
				return true;
			}
		} 
		catch (Exception e)
		{
			return false;
		}	
	}
	
	private JsonObject createNewPlayerVars(int defId, int[] values) throws JsonSyntaxException, UnsupportedEncodingException, IOException
	{
		//create a new player var with the value specified
		
		JsonObject object = new JsonObject();
		object.addProperty("defId", defId);
		
		JsonObject valuesTypeLevel = new JsonObject();
		JsonObject valuesStore = new JsonObject();
		
	    int index = 0;
	    JsonObject varArray = new JsonObject();
	    
	    for(int value : values)
	    {
	    	varArray.addProperty(String.valueOf(index), value);
	    	index++;
	    }
	    
	    //convert this to a string object and store it in values
	    valuesStore.addProperty("values", varArray.toString());
	    
	    //need to know what the value type is...
	    
	    var type = getVarType(defId);
	    
	    valuesTypeLevel.add(type.componentName, valuesStore);
	    object.add("components", valuesTypeLevel);
	    
		object.addProperty("id", UUID.randomUUID().toString());
		object.addProperty("type", 303);
	    
	    return object;
	}
	
	private UserVarType getVarType(int defId) throws JsonSyntaxException, UnsupportedEncodingException, IOException
	{
		// Load helper
		InputStream strm = PlayerSettingsAccessor.class.getClassLoader()
				.getResourceAsStream("userVars.json");
		JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8"))
				.getAsJsonObject().get("userVars").getAsJsonObject();
		strm.close();
		
		//find var by def id
		
		var varDef = helper.get(String.valueOf(defId));
		var componentArray = varDef.getAsJsonObject().get("data").getAsJsonObject().get("components").getAsJsonArray();
		var component = componentArray.get(0);
		var typeVal = component.getAsJsonObject().get("componentJSON").getAsJsonObject().get("type").getAsInt();
		
		UserVarType type = null;
		
		for(var member : UserVarType.values()) {
			if(member.val == typeVal)
			{
				type = member;
				break;
			}
		}
		
		return type;
	}
	
	
}
