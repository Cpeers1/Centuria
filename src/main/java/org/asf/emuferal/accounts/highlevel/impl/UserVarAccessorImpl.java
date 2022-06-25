package org.asf.emuferal.accounts.highlevel.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.UserVarAccessor;
import org.asf.emuferal.entities.inventory.playervars.PlayerVarValue;
import org.asf.emuferal.entities.systems.playervars.SetPlayerVarResult;
import org.asf.emuferal.enums.inventory.UserVarType;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class UserVarAccessorImpl extends UserVarAccessor {

	public UserVarAccessorImpl(PlayerInventory inventory) {
		super(inventory);
	}

	private JsonObject createNewPlayerVars(int defId, int[] values)
			throws JsonSyntaxException, UnsupportedEncodingException, IOException {
		// create a new player var with the value specified

		JsonObject object = new JsonObject();
		object.addProperty("defId", defId);

		JsonObject valuesTypeLevel = new JsonObject();
		JsonObject valuesStore = new JsonObject();

		int index = 0;
		JsonObject varArray = new JsonObject();

		for (int value : values) {
			varArray.addProperty(String.valueOf(index), value);
			index++;
		}

		// convert this to a string object and store it in values
		valuesStore.addProperty("values", varArray.toString());

		// need to know what the value type is...

		var type = getVarType(defId);

		valuesTypeLevel.add(type.componentName, valuesStore);
		object.add("components", valuesTypeLevel);

		object.addProperty("id", UUID.randomUUID().toString());
		object.addProperty("type", 303);

		return object;
	}

	private JsonObject createNewPlayerVars(int defId, HashMap<Integer, Integer> values)
			throws JsonSyntaxException, UnsupportedEncodingException, IOException {
		// create a new player var with the values at the indexes specified

		JsonObject object = new JsonObject();
		object.addProperty("defId", defId);

		JsonObject valuesTypeLevel = new JsonObject();
		JsonObject valuesStore = new JsonObject();

		JsonObject varArray = new JsonObject();

		for (int i = 0; i < values.size(); i++) {
			//varArray.addProperty(String.valueOf(values.keySet()), value);
		}

		// convert this to a string object and store it in values
		valuesStore.addProperty("values", varArray.toString());

		// need to know what the value type is...

		var type = getVarType(defId);

		valuesTypeLevel.add(type.componentName, valuesStore);
		object.add("components", valuesTypeLevel);

		object.addProperty("id", UUID.randomUUID().toString());
		object.addProperty("type", 303);

		return object;
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
	public SetPlayerVarResult setPlayerVarValue(int defID, int[] values) {
		try {

			if (!inventory.getAccessor().hasInventoryObject("303", defID)) {
				// create the inventory object
				JsonObject newVarObject = createNewPlayerVars(defID, values);

				var inv = inventory.getItem("303");
				// UHH
				inv.getAsJsonArray().add(newVarObject);

				inventory.setItem("303", inv);

				var outputInv = new JsonArray();
				outputInv.add(newVarObject);
				var output = new SetPlayerVarResult(true, outputInv);

				return output;
			} else {
				var inv = inventory.getItem("303");

				// find the item

				JsonElement element = null;

				for (var item : inv.getAsJsonArray()) {
					if (item.getAsJsonObject().get("defId").getAsInt() == defID) {
						element = item;
						break;
					}
				}

				if (element == null) {
					var output = new SetPlayerVarResult(false, null);

					return output; // cannot find
				}

				String typeName = getVarType(defID).componentName;

				var sortThrough = element.getAsJsonObject().get("components").getAsJsonObject().get(typeName)
						.getAsJsonObject();

				sortThrough.remove("values");

				sortThrough.add("values", element);

				int index = 0;
				JsonObject varArray = new JsonObject();

				for (int value : values) {
					varArray.addProperty(String.valueOf(index), value);
					index++;
				}

				// convert this to a string object and store it in values
				sortThrough.addProperty("values", varArray.toString());

				var outputVarInv = new JsonArray();
				outputVarInv.add(element);

				inventory.setItem("303", inv);

				var output = new SetPlayerVarResult(true, outputVarInv);

				return output;
			}
		} catch (Exception e) {
			var output = new SetPlayerVarResult(false, null);

			return output;
		}
	}

	@Override
	public SetPlayerVarResult setPlayerVarValue(int defID, int index, int value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SetPlayerVarResult setPlayerVarValue(int defID, HashMap<Integer, Integer> indexToValueUpdateMap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PlayerVarValue[] getPlayerVarValue(int defID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PlayerVarValue getPlayerVarValue(int defID, int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PlayerVarValue[] getPlayerVarValue(int defID, int[] indexes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deletePlayerVar(int defID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SetPlayerVarResult deletePlayerVarValueAtIndex(int defID, int index) {
		// TODO Auto-generated method stub
		return null;
	}

}
