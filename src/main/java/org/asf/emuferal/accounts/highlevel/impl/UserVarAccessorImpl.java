package org.asf.emuferal.accounts.highlevel.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.UserVarAccessor;
import org.asf.emuferal.entities.inventory.uservars.UserVarValue;
import org.asf.emuferal.entities.systems.uservars.SetUserVarResult;
import org.asf.emuferal.enums.inventory.uservars.UserVarType;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import org.asf.emuferal.entities.inventory.uservars.*;
import org.asf.emuferal.entities.inventory.components.InventoryItemComponent;
import org.asf.emuferal.entities.inventory.components.uservars.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class UserVarAccessorImpl extends UserVarAccessor {

	public UserVarAccessorImpl(PlayerInventory inventory) {
		super(inventory);
	}

	private UserVarItem createNewUserVar(int defId, UserVarValue[] values)
			throws JsonSyntaxException, UnsupportedEncodingException, IOException {
		// create a new player var with the value specified
		
		var type = getVarType(defId);
		var userVarItem = new UserVarItem(type);

		UserVarComponent userVarComponent = null;
		
		switch(type)
		{
			case Any:
				userVarComponent = new UserVarCustomComponent();
				break;
			case Bit:
				userVarComponent = new UserVarBitComponent();
				break;
			case BitOnOnly:
				userVarComponent = new UserVarBitOnOnlyComponent();
				break;
			case Counter:
				userVarComponent = new UserVarCounterComponent();
				break;
			case Highest:
				userVarComponent = new UserVarHighestComponent();
				break;
			case Lowest:
				userVarComponent = new UserVarLowestComponent();
				break;
			default:
				break;					
		}
		
		for(var value : values)
		{
			userVarComponent.setUserVarValue(value);
		}
		
		userVarItem.setUserVarComponent(userVarComponent);
		userVarItem.defId = defId;
		userVarItem.invType = 303;
		userVarItem.uuid = UUID.randomUUID().toString();

		return userVarItem;
	}

	private UserVarType getVarType(int defId) throws JsonSyntaxException, UnsupportedEncodingException, IOException {
		// Load helper
		InputStream strm = UserVarAccessorImpl.class.getClassLoader().getResourceAsStream("userVars.json");
		JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
				.get("userVars").getAsJsonObject();
		strm.close();

		// find var by def id

		var varDef = helper.get(String.valueOf(defId));
		var componentArray = varDef.getAsJsonObject().get("data").getAsJsonObject().get("components").getAsJsonArray();
		var component = componentArray.get(0);
		var typeVal = component.getAsJsonObject().get("componentJSON").getAsJsonObject().get("type").getAsInt();

		UserVarType type = null;

		for (var member : UserVarType.values()) {
			if (member.val == typeVal) {
				type = member;
				break;
			}
		}

		return type;
	}

	@Override
	public SetUserVarResult setPlayerVarValue(int defID, int[] values) {
		try {

			if (!inventory.getAccessor().hasInventoryObject("303", defID)) {
				// create the inventory object
				
				UserVarValue[] userVarValues = new UserVarValue[values.length];
				
				for(int i = 0; i < values.length; i++)
				{
					userVarValues[i] = new UserVarValue();
					userVarValues[i].index = i;
					userVarValues[i].value = values[i];
				}
				
				var newVarObject = createNewUserVar(defID, userVarValues);

				var inv = inventory.getItem("303");
				// UHH
				inv.getAsJsonArray().add(newVarObject.toJsonObject());

				inventory.setItem("303", inv);

				var output = new SetUserVarResult(true, new UserVarItem[] { newVarObject });

				return output;
			} else {
				var inv = inventory.getItem("303");

				// find the item

				JsonElement element = null;
				int index = 0;

				for (var item : inv.getAsJsonArray()) {
					if (item.getAsJsonObject().get("defId").getAsInt() == defID) {
						element = item;
						break;
					}
					index++;
				}

				if (element == null) {
					var output = new SetUserVarResult(false, null);

					return output; // cannot find
				}

				var type = getVarType(defID);
				

				var sortThrough = element.getAsJsonObject().get("components").getAsJsonObject().get(type.componentName)
						.getAsJsonObject();
							
				UserVarItem userVarItem = new UserVarItem(type);
				userVarItem.defId = defID;
				userVarItem.invType = 303;
				userVarItem.uuid = element.getAsJsonObject().get("id").getAsString();
				
				UserVarComponent userVarComponent = null;
				switch(type)
				{
					case Any:
						userVarComponent = (UserVarComponent)InventoryItemComponent.fromJson(UserVarCustomComponent.class, sortThrough);
						break;
					case Bit:
						userVarComponent = (UserVarComponent)InventoryItemComponent.fromJson(UserVarBitComponent.class, sortThrough);
						break;
					case BitOnOnly:
						userVarComponent = (UserVarComponent)InventoryItemComponent.fromJson(UserVarBitOnOnlyComponent.class, sortThrough);
						break;
					case Counter:
						userVarComponent = (UserVarComponent)InventoryItemComponent.fromJson(UserVarCounterComponent.class, sortThrough);
						break;
					case Highest:
						userVarComponent = (UserVarComponent)InventoryItemComponent.fromJson(UserVarHighestComponent.class, sortThrough);
						break;
					case Lowest:
						userVarComponent = (UserVarComponent)InventoryItemComponent.fromJson(UserVarLowestComponent.class, sortThrough);
						break;				
				}

				for(int i = 0; i < values.length; i++)
				{
					var userVarValue = new UserVarValue();
					userVarValue.index = i;
					userVarValue.value = values[i];
					
					userVarComponent.setUserVarValue(userVarValue);
				}	
				
				userVarItem.setUserVarComponent(userVarComponent);
				
				var object = userVarItem.toJsonObject();
				inv.getAsJsonArray().set(index, object);

				var outputVarInv = new JsonArray();
				outputVarInv.add(object);

				inventory.setItem("303", inv);

				return null;
			}
		} catch (Exception e) {
			var output = new SetUserVarResult(false, null);

			return output;
		}
	}

	@Override
	public SetUserVarResult setPlayerVarValue(int defID, int index, int value) {
		try {

			if (!inventory.getAccessor().hasInventoryObject("303", defID)) {
				// create the inventory object
				
				UserVarValue[] userVarValues = new UserVarValue[1];
				
				userVarValues[0] = new UserVarValue();
				userVarValues[0].index = index;
				userVarValues[0].value = value;
			
				var newVarObject = createNewUserVar(defID, userVarValues);

				var inv = inventory.getItem("303");
				// UHH
				inv.getAsJsonArray().add(newVarObject.toJsonObject());

				inventory.setItem("303", inv);

				var output = new SetUserVarResult(true, new UserVarItem[] { newVarObject });

				return output;
			} else {
				var inv = inventory.getItem("303");

				// find the item

				JsonElement element = null;
				int elementIndex = 0;

				for (var item : inv.getAsJsonArray()) {
					if (item.getAsJsonObject().get("defId").getAsInt() == defID) {
						element = item;
						break;
					}
					elementIndex++;
				}

				if (element == null) {
					var output = new SetUserVarResult(false, null);

					return output; // cannot find
				}

				var type = getVarType(defID);
				

				var sortThrough = element.getAsJsonObject().get("components").getAsJsonObject().get(type.componentName)
						.getAsJsonObject();
							
				UserVarItem userVarItem = new UserVarItem(type);
				userVarItem.defId = defID;
				userVarItem.invType = 303;
				userVarItem.uuid = element.getAsJsonObject().get("id").getAsString();
				
				UserVarComponent userVarComponent = null;
				switch(type)
				{
					case Any:
						userVarComponent = (UserVarComponent)InventoryItemComponent.fromJson(UserVarCustomComponent.class, sortThrough);
						break;
					case Bit:
						userVarComponent = (UserVarComponent)InventoryItemComponent.fromJson(UserVarBitComponent.class, sortThrough);
						break;
					case BitOnOnly:
						userVarComponent = (UserVarComponent)InventoryItemComponent.fromJson(UserVarBitOnOnlyComponent.class, sortThrough);
						break;
					case Counter:
						userVarComponent = (UserVarComponent)InventoryItemComponent.fromJson(UserVarCounterComponent.class, sortThrough);
						break;
					case Highest:
						userVarComponent = (UserVarComponent)InventoryItemComponent.fromJson(UserVarHighestComponent.class, sortThrough);
						break;
					case Lowest:
						userVarComponent = (UserVarComponent)InventoryItemComponent.fromJson(UserVarLowestComponent.class, sortThrough);
						break;				
				}


				var userVarValue = new UserVarValue();
				userVarValue.index = index;
				userVarValue.value = value;
				
				userVarComponent.setUserVarValue(userVarValue);
				
				userVarItem.setUserVarComponent(userVarComponent);
				
				var object = userVarItem.toJsonObject();
				inv.getAsJsonArray().set(elementIndex, object);

				inventory.setItem("303", inv);

				return new SetUserVarResult(true, new UserVarItem[] { userVarItem });
			}
		} catch (Exception e) {
			var output = new SetUserVarResult(false, null);

			return output;
		}
	}

	@Override
	public SetUserVarResult setPlayerVarValue(int defID, HashMap<Integer, Integer> indexToValueUpdateMap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserVarValue[] getPlayerVarValue(int defID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserVarValue getPlayerVarValue(int defID, int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserVarValue[] getPlayerVarValue(int defID, int[] indexes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deletePlayerVar(int defID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SetUserVarResult deletePlayerVarValueAtIndex(int defID, int index) {
		// TODO Auto-generated method stub
		return null;
	}

}
