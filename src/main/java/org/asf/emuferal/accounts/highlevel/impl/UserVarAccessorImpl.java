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
import org.asf.emuferal.entities.uservars.SetUserVarResult;
import org.asf.emuferal.entities.uservars.UserVarValue;
import org.asf.emuferal.enums.uservars.UserVarType;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import org.asf.emuferal.entities.inventory.uservars.*;
import org.asf.emuferal.entities.components.InventoryItemComponent;
import org.asf.emuferal.entities.components.uservars.*;
import org.asf.emuferal.entities.inventory.InventoryItem;

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

		switch (type) {
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

		for (var value : values) {
			userVarComponent.setUserVarValue(value);
		}

		userVarItem.setUserVarComponent(userVarComponent);
		userVarItem.defId = defId;
		userVarItem.invType = 303;
		userVarItem.uuid = UUID.randomUUID().toString();

		return userVarItem;
	}

	private UserVarType getVarType(int defId) {
		try {
			// Load helper
			InputStream strm = UserVarAccessorImpl.class.getClassLoader().getResourceAsStream("userVars.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("userVars").getAsJsonObject();
			strm.close();

			// find var by def id

			var varDef = helper.get(String.valueOf(defId));
			var componentArray = varDef.getAsJsonObject().get("data").getAsJsonObject().get(InventoryItem.componentsPropertyName).getAsJsonArray();
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
		catch(Exception exception)
		{
			throw new RuntimeException(exception);
		}

	}

	@Override
	public SetUserVarResult setPlayerVarValue(int defID, int[] values) {
		try {

			if (!inventory.getAccessor().hasInventoryObject(Integer.toString(UserVarItem.InvType), defID)) {
				// create the inventory object

				UserVarValue[] userVarValues = new UserVarValue[values.length];

				for (int i = 0; i < values.length; i++) {
					userVarValues[i] = new UserVarValue();
					userVarValues[i].index = i;
					userVarValues[i].value = values[i];
				}

				var newVarObject = createNewUserVar(defID, userVarValues);

				var inv = inventory.getItem(Integer.toString(UserVarItem.InvType));
				// UHH
				inv.getAsJsonArray().add(newVarObject.toJsonObject());

				inventory.setItem(Integer.toString(UserVarItem.InvType), inv);

				var output = new SetUserVarResult(true, new UserVarItem[] { newVarObject });

				return output;
			} else {
				var inv = inventory.getItem(Integer.toString(UserVarItem.InvType));

				// find the item

				JsonElement element = null;
				int index = 0;

				for (var item : inv.getAsJsonArray()) {
					if (item.getAsJsonObject().get(InventoryItem.defIdPropertyName).getAsInt() == defID) {
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

				UserVarItem userVarItem = new UserVarItem(type);
				userVarItem.fromJsonObject(element.getAsJsonObject());

				UserVarComponent userVarComponent = userVarItem.getUserVarComponent();
				for (int i = 0; i < values.length; i++) {
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

				inventory.setItem(Integer.toString(UserVarItem.InvType), inv);

				return null;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public SetUserVarResult setPlayerVarValue(int defID, int index, int value) {
		try {

			if (!inventory.getAccessor().hasInventoryObject(Integer.toString(UserVarItem.InvType), defID)) {
				// create the inventory object

				UserVarValue[] userVarValues = new UserVarValue[1];

				userVarValues[0] = new UserVarValue();
				userVarValues[0].index = index;
				userVarValues[0].value = value;

				var newVarObject = createNewUserVar(defID, userVarValues);

				var inv = inventory.getItem(Integer.toString(UserVarItem.InvType));
				// UHH
				inv.getAsJsonArray().add(newVarObject.toJsonObject());

				inventory.setItem(Integer.toString(UserVarItem.InvType), inv);

				var output = new SetUserVarResult(true, new UserVarItem[] { newVarObject });

				return output;
			} else {
				var inv = inventory.getItem(Integer.toString(UserVarItem.InvType));

				// find the item

				JsonElement element = null;
				int elementIndex = 0;

				for (var item : inv.getAsJsonArray()) {
					if (item.getAsJsonObject().get(InventoryItem.defIdPropertyName).getAsInt() == defID) {
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

				UserVarItem userVarItem = new UserVarItem(type);
				userVarItem.fromJsonObject(element.getAsJsonObject());

				UserVarComponent userVarComponent = userVarItem.getUserVarComponent();

				var userVarValue = new UserVarValue();
				userVarValue.index = index;
				userVarValue.value = value;

				userVarComponent.setUserVarValue(userVarValue);

				userVarItem.setUserVarComponent(userVarComponent);

				var object = userVarItem.toJsonObject();
				inv.getAsJsonArray().set(elementIndex, object);

				inventory.setItem(Integer.toString(UserVarItem.InvType), inv);

				return new SetUserVarResult(true, new UserVarItem[] { userVarItem });
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public SetUserVarResult setPlayerVarValue(int defID, HashMap<Integer, Integer> indexToValueUpdateMap) {
		try {

			if (!inventory.getAccessor().hasInventoryObject(Integer.toString(UserVarItem.InvType), defID)) {
				// create the inventory object

				UserVarValue[] userVarValues = new UserVarValue[indexToValueUpdateMap.size()];

				int index = 0;
				for(var indexToValueUpdate : indexToValueUpdateMap.entrySet())
				{
					userVarValues[index] = new UserVarValue();
					userVarValues[index].index = indexToValueUpdate.getKey();
					userVarValues[index].value = indexToValueUpdate.getValue();
					
					index++;
				}

				var newVarObject = createNewUserVar(defID, userVarValues);

				var inv = inventory.getItem(Integer.toString(UserVarItem.InvType));
				// UHH
				inv.getAsJsonArray().add(newVarObject.toJsonObject());

				inventory.setItem(Integer.toString(UserVarItem.InvType), inv);

				var output = new SetUserVarResult(true, new UserVarItem[] { newVarObject });

				return output;
			} else {
				var inv = inventory.getItem(Integer.toString(UserVarItem.InvType));

				// find the item

				JsonElement element = null;
				int elementIndex = 0;

				for (var item : inv.getAsJsonArray()) {
					if (item.getAsJsonObject().get(InventoryItem.defIdPropertyName).getAsInt() == defID) {
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

				UserVarItem userVarItem = new UserVarItem(type);
				userVarItem.fromJsonObject(element.getAsJsonObject());

				UserVarComponent userVarComponent = userVarItem.getUserVarComponent();

				for(var indexToValueUpdate : indexToValueUpdateMap.entrySet())
				{
					var userVarValue = new UserVarValue();
					userVarValue.index = indexToValueUpdate.getKey();
					userVarValue.value = indexToValueUpdate.getValue();		
					
					userVarComponent.setUserVarValue(userVarValue);
				}

				userVarItem.setUserVarComponent(userVarComponent);

				var object = userVarItem.toJsonObject();
				inv.getAsJsonArray().set(elementIndex, object);

				inventory.setItem(Integer.toString(UserVarItem.InvType), inv);

				return new SetUserVarResult(true, new UserVarItem[] { userVarItem });
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public UserVarValue[] getPlayerVarValue(int defID) {
		try {
			//Can't access anything if the inventory is null.
			if (!inventory.getAccessor().hasInventoryObject(Integer.toString(UserVarItem.InvType), defID)) return null;

			var inv = inventory.getItem(Integer.toString(UserVarItem.InvType));

			// find the item

			JsonElement element = null;

			for (var item : inv.getAsJsonArray()) {
				if (item.getAsJsonObject().get(InventoryItem.defIdPropertyName).getAsInt() == defID) {
					element = item;
					break;
				}
			}
			
			if (element == null) {
				return null; // cannot find
			}
			
			var type = getVarType(defID);

			UserVarItem userVarItem = new UserVarItem(type);
			userVarItem.fromJsonObject(element.getAsJsonObject());

			UserVarComponent userVarComponent = userVarItem.getUserVarComponent();
			
			return userVarComponent.getAllUserVarValues();
		}
		catch(Exception exception)
		{
			throw new RuntimeException(exception);
		}
	}

	@Override
	public UserVarValue getPlayerVarValue(int defID, int index) {
		try {
			//Can't access anything if the inventory is null.
			if (!inventory.getAccessor().hasInventoryObject(Integer.toString(UserVarItem.InvType), defID)) return null;

			var inv = inventory.getItem(Integer.toString(UserVarItem.InvType));

			// find the item

			JsonElement element = null;

			for (var item : inv.getAsJsonArray()) {
				if (item.getAsJsonObject().get(InventoryItem.defIdPropertyName).getAsInt() == defID) {
					element = item;
					break;
				}
			}
			
			if (element == null) {
				return null; // cannot find
			}
			
			var type = getVarType(defID);

			UserVarItem userVarItem = new UserVarItem(defID, element.getAsJsonObject().get(InventoryItem.uuidPropertyName).getAsString(), type);

			UserVarComponent userVarComponent = userVarItem.getUserVarComponent();

			return userVarComponent.getUserVarValue(index);
		}
		catch(Exception exception)
		{
			throw new RuntimeException(exception);
		}
	}

	@Override
	public UserVarValue[] getPlayerVarValue(int defID, int[] indexes) {
		try {
			//Can't access anything if the inventory is null.
			if (!inventory.getAccessor().hasInventoryObject(Integer.toString(UserVarItem.InvType), defID)) return null;

			var inv = inventory.getItem(Integer.toString(UserVarItem.InvType));

			// find the item

			JsonElement element = null;

			for (var item : inv.getAsJsonArray()) {
				if (item.getAsJsonObject().get(InventoryItem.defIdPropertyName).getAsInt() == defID) {
					element = item;
					break;
				}
			}
			
			if (element == null) {
				return null; // cannot find
			}
			
			var type = getVarType(defID);

			UserVarItem userVarItem = new UserVarItem(defID, element.getAsJsonObject().get(InventoryItem.uuidPropertyName).getAsString(), type);

			UserVarComponent userVarComponent = userVarItem.getUserVarComponent();
			
			var userVarValues = new ArrayList<UserVarValue>();
			
			for(int index : indexes)
			{
				userVarValues.add(userVarComponent.getUserVarValue(index));
			}
			
			return (UserVarValue[])userVarValues.toArray();
		}
		catch(Exception exception)
		{
			throw new RuntimeException(exception);
		}
	}

	@Override
	public boolean deletePlayerVar(int defID) {
		try {
			//Can't access anything if the inventory is null.
			if (!inventory.getAccessor().hasInventoryObject(Integer.toString(UserVarItem.InvType), defID)) return false;

			inventory.getAccessor().removeInventoryObject(Integer.toString(UserVarItem.InvType), defID);
			
			return true;
		}
		catch(Exception exception)
		{
			throw new RuntimeException(exception);
		}
	}

	@Override
	public boolean deletePlayerVarValueAtIndex(int defID, int index) {
		try {
			//Can't access anything if the inventory is null.
			if (!inventory.getAccessor().hasInventoryObject(Integer.toString(UserVarItem.InvType), defID)) return false;

			var inv = inventory.getItem(Integer.toString(UserVarItem.InvType));

			// find the item

			JsonElement element = null;

			for (var item : inv.getAsJsonArray()) {
				if (item.getAsJsonObject().get(InventoryItem.defIdPropertyName).getAsInt() == defID) {
					element = item;
					break;
				}
			}
			
			if (element == null) {
				return false; // cannot find
			}
			
			var type = getVarType(defID);

			UserVarItem userVarItem = new UserVarItem(defID, element.getAsJsonObject().get(InventoryItem.uuidPropertyName).getAsString(), type);

			UserVarComponent userVarComponent = userVarItem.getUserVarComponent();
			
			userVarComponent.deleteUserVarValue(index);

			return true;
		}
		catch(Exception exception)
		{
			throw new RuntimeException(exception);
		}
	}

}
