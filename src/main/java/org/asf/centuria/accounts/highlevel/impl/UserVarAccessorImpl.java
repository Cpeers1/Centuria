package org.asf.centuria.accounts.highlevel.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.highlevel.UserVarAccessor;
import org.asf.centuria.entities.uservars.SetUserVarResult;
import org.asf.centuria.entities.uservars.UserVarValue;
import org.asf.centuria.enums.uservars.UserVarType;
import org.asf.centuria.entities.components.uservars.*;
import org.asf.centuria.entities.inventoryitems.InventoryItem;
import org.asf.centuria.entities.inventoryitems.uservars.UserVarItem;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class UserVarAccessorImpl extends UserVarAccessor {

	private static JsonObject helper = null;

	public UserVarAccessorImpl(PlayerInventory inventory) {
		super(inventory);

		if (helper != null)
			return;

		try {

			InputStream strm = UserVarAccessorImpl.class.getClassLoader()
					.getResourceAsStream("itemlists/uservars.json");
			helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject().get("UserVars")
					.getAsJsonObject();
			strm.close();
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}

	private UserVarItem createNewUserVar(int defId, UserVarValue[] values)
			throws JsonSyntaxException, UnsupportedEncodingException, IOException {
		// create a new player var with the value specified

		var type = getVarType(defId);
		var userVarItem = new UserVarItem(defId, UUID.randomUUID().toString(), type);

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

		return userVarItem;
	}

	private UserVarType getVarType(int defId) {
		try {
			// find var by def id
			var varDef = helper.get(String.valueOf(defId)).getAsJsonObject();
			var typeVal = varDef.get("type").getAsInt();

			UserVarType type = null;

			for (var member : UserVarType.values()) {
				if (member.val == typeVal) {
					type = member;
					break;
				}
			}

			return type;
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}

	}

	@Override
	public SetUserVarResult setPlayerVarValue(int defID, int[] values) {
		try {

			if (!inventory.getAccessor().hasInventoryObject(Integer.toString(UserVarItem.INV_TYPE.invTypeId), defID)) {
				// create the inventory object

				UserVarValue[] userVarValues = new UserVarValue[values.length];

				for (int i = 0; i < values.length; i++) {
					userVarValues[i] = new UserVarValue();
					userVarValues[i].index = i;
					userVarValues[i].value = values[i];
				}

				var newVarObject = createNewUserVar(defID, userVarValues);

				var inv = inventory.getItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId));
				// UHH
				inv.getAsJsonArray().add(newVarObject.toJsonObject());

				inventory.setItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId), inv);

				var output = new SetUserVarResult(true, new UserVarItem[] { newVarObject });

				return output;
			} else {
				var inv = inventory.getItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId));

				// find the item

				JsonElement element = null;
				int elementIndex = 0;

				for (var item : inv.getAsJsonArray()) {
					if (item.getAsJsonObject().get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsInt() == defID) {
						element = item;
						break;
					}
					elementIndex++;
				}
				
				UserVarValue[] userVarValues = new UserVarValue[values.length];
				for(int i = 0; i < values.length; i++)
				{
					var userVarValue = new UserVarValue();
					userVarValue.index = i;
					userVarValue.value = values[i];
					userVarValues[i] = userVarValue;
				}

				if (element == null) {
					var userVarItem = createNewUserVar(defID, userVarValues);
					element = userVarItem.toJsonObject();
					inv.getAsJsonArray().set(elementIndex, element);

					var outputVarInv = new JsonArray();
					outputVarInv.add(element);

					inventory.setItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId), inv);
					
					return new SetUserVarResult(true, new UserVarItem[] { userVarItem });
				}
				else
				{
					var type = getVarType(defID);

					UserVarItem userVarItem = new UserVarItem(type);
					userVarItem.fromJsonObject(element.getAsJsonObject());
					
					UserVarComponent userVarComponent = userVarItem.getUserVarComponent();
					for (int i = 0; i < userVarValues.length; i++) {
						userVarComponent.setUserVarValue(userVarValues[i]);
					}
					
					userVarItem.setUserVarComponent(userVarComponent);
					
					var object = userVarItem.toJsonObject();
					inv.getAsJsonArray().set(elementIndex, object);

					var outputVarInv = new JsonArray();
					outputVarInv.add(object);

					inventory.setItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId), inv);
					
					return new SetUserVarResult(true, new UserVarItem[] { userVarItem });
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public SetUserVarResult setPlayerVarValue(int defID, int index, int value) {
		try {
			
			UserVarValue[] userVarValues = new UserVarValue[1];

			userVarValues[0] = new UserVarValue();
			userVarValues[0].index = index;
			userVarValues[0].value = value;
			
			if (!inventory.getAccessor().hasInventoryObject(Integer.toString(UserVarItem.INV_TYPE.invTypeId), defID)) {
				// create the inventory object

				var newVarObject = createNewUserVar(defID, userVarValues);

				var inv = inventory.getItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId));
				// UHH
				inv.getAsJsonArray().add(newVarObject.toJsonObject());

				inventory.setItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId), inv);

				var output = new SetUserVarResult(true, new UserVarItem[] { newVarObject });

				return output;
			} else {
				var inv = inventory.getItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId));

				// find the item

				JsonElement element = null;
				int elementIndex = 0;

				for (var item : inv.getAsJsonArray()) {
					if (item.getAsJsonObject().get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsInt() == defID) {
						element = item;
						break;
					}
					elementIndex++;
				}
				
				if (element == null) {
					var userVarItem = createNewUserVar(defID, userVarValues);
					element = userVarItem.toJsonObject();
					inv.getAsJsonArray().set(elementIndex, element);

					var outputVarInv = new JsonArray();
					outputVarInv.add(element);

					inventory.setItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId), inv);
					
					return new SetUserVarResult(true, new UserVarItem[] { userVarItem });
				}
				else
				{
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

					inventory.setItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId), inv);

					return new SetUserVarResult(true, new UserVarItem[] { userVarItem });				
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public SetUserVarResult setPlayerVarValue(int defID, HashMap<Integer, Integer> indexToValueUpdateMap) {
		try {
			if (!inventory.getAccessor().hasInventoryObject(Integer.toString(UserVarItem.INV_TYPE.invTypeId), defID)) {
				// create the inventory object

				UserVarValue[] userVarValues = new UserVarValue[indexToValueUpdateMap.size()];

				int index = 0;
				for (var indexToValueUpdate : indexToValueUpdateMap.entrySet()) {
					userVarValues[index] = new UserVarValue();
					userVarValues[index].index = indexToValueUpdate.getKey();
					userVarValues[index].value = indexToValueUpdate.getValue();

					index++;
				}

				var newVarObject = createNewUserVar(defID, userVarValues);

				var inv = inventory.getItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId));
				// UHH
				inv.getAsJsonArray().add(newVarObject.toJsonObject());

				inventory.setItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId), inv);

				var output = new SetUserVarResult(true, new UserVarItem[] { newVarObject });

				return output;
			} else {
				var inv = inventory.getItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId));

				// find the item

				JsonElement element = null;
				int elementIndex = 0;

				for (var item : inv.getAsJsonArray()) {
					if (item.getAsJsonObject().get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsInt() == defID) {
						element = item;
						break;
					}
					elementIndex++;
				}

				if (element == null) {
					
					UserVarValue[] userVarValues = new UserVarValue[indexToValueUpdateMap.size()];
					int indexB = 0;
					for (var indexToValueUpdate : indexToValueUpdateMap.entrySet()) {
						var userVarValue = new UserVarValue();
						userVarValue.index = indexToValueUpdate.getKey();
						userVarValue.value = indexToValueUpdate.getValue();
						userVarValues[indexB] = userVarValue;
						
						indexB++;
					}
					
					var userVarItem = createNewUserVar(defID, userVarValues);
					element = userVarItem.toJsonObject();
					inv.getAsJsonArray().set(elementIndex, element);

					var outputVarInv = new JsonArray();
					outputVarInv.add(element);

					inventory.setItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId), inv);
					
					return new SetUserVarResult(true, new UserVarItem[] { userVarItem });
				}
				else {
					var type = getVarType(defID);

					UserVarItem userVarItem = new UserVarItem(type);
					userVarItem.fromJsonObject(element.getAsJsonObject());

					UserVarComponent userVarComponent = userVarItem.getUserVarComponent();

					for (var indexToValueUpdate : indexToValueUpdateMap.entrySet()) {
						var userVarValue = new UserVarValue();
						userVarValue.index = indexToValueUpdate.getKey();
						userVarValue.value = indexToValueUpdate.getValue();

						userVarComponent.setUserVarValue(userVarValue);
					}

					userVarItem.setUserVarComponent(userVarComponent);

					var object = userVarItem.toJsonObject();
					inv.getAsJsonArray().set(elementIndex, object);

					inventory.setItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId), inv);

					return new SetUserVarResult(true, new UserVarItem[] { userVarItem });				
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public UserVarValue[] getPlayerVarValue(int defID) {
		try {
			// Can't access anything if the inventory is null.
			if (!inventory.getAccessor().hasInventoryObject(Integer.toString(UserVarItem.INV_TYPE.invTypeId), defID))
				return null;

			var inv = inventory.getItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId));

			// find the item

			JsonElement element = null;

			for (var item : inv.getAsJsonArray()) {
				if (item.getAsJsonObject().get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsInt() == defID) {
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
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public UserVarValue getPlayerVarValue(int defID, int index) {
		try {
			// Can't access anything if the inventory is null.
			if (!inventory.getAccessor().hasInventoryObject(Integer.toString(UserVarItem.INV_TYPE.invTypeId), defID))
				return null;

			var inv = inventory.getItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId));

			// find the item

			JsonElement element = null;

			for (var item : inv.getAsJsonArray()) {
				if (item.getAsJsonObject().get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsInt() == defID) {
					element = item;
					break;
				}
			}

			if (element == null) {
				return null; // cannot find
			}

			var type = getVarType(defID);

			UserVarItem userVarItem = new UserVarItem(defID,
					element.getAsJsonObject().get(InventoryItem.UUID_PROPERTY_NAME).getAsString(), type);
			userVarItem.fromJsonObject(element.getAsJsonObject());

			UserVarComponent userVarComponent = userVarItem.getUserVarComponent();

			return userVarComponent.getUserVarValue(index);
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public UserVarValue[] getPlayerVarValue(int defID, int[] indexes) {
		try {
			// Can't access anything if the inventory is null.
			if (!inventory.getAccessor().hasInventoryObject(Integer.toString(UserVarItem.INV_TYPE.invTypeId), defID))
				return null;

			var inv = inventory.getItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId));

			// find the item

			JsonElement element = null;

			for (var item : inv.getAsJsonArray()) {
				if (item.getAsJsonObject().get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsInt() == defID) {
					element = item;
					break;
				}
			}

			if (element == null) {
				return null; // cannot find
			}

			var type = getVarType(defID);

			UserVarItem userVarItem = new UserVarItem(defID,
					element.getAsJsonObject().get(InventoryItem.UUID_PROPERTY_NAME).getAsString(), type);
			userVarItem.fromJsonObject(element.getAsJsonObject());

			UserVarComponent userVarComponent = userVarItem.getUserVarComponent();

			var userVarValues = new ArrayList<UserVarValue>();

			for (int index : indexes) {
				userVarValues.add(userVarComponent.getUserVarValue(index));
			}

			return (UserVarValue[]) userVarValues.toArray();
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public boolean deletePlayerVar(int defID) {
		try {
			// Can't access anything if the inventory is null.
			if (!inventory.getAccessor().hasInventoryObject(Integer.toString(UserVarItem.INV_TYPE.invTypeId), defID))
				return false;

			inventory.getAccessor().removeInventoryObject(Integer.toString(UserVarItem.INV_TYPE.invTypeId), defID);

			return true;
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public boolean deletePlayerVarValueAtIndex(int defID, int index) {
		try {
			// Can't access anything if the inventory is null.
			if (!inventory.getAccessor().hasInventoryObject(Integer.toString(UserVarItem.INV_TYPE.invTypeId), defID))
				return false;

			var inv = inventory.getItem(Integer.toString(UserVarItem.INV_TYPE.invTypeId));

			// find the item

			JsonElement element = null;

			for (var item : inv.getAsJsonArray()) {
				if (item.getAsJsonObject().get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsInt() == defID) {
					element = item;
					break;
				}
			}

			if (element == null) {
				return false; // cannot find
			}

			var type = getVarType(defID);

			UserVarItem userVarItem = new UserVarItem(defID,
					element.getAsJsonObject().get(InventoryItem.UUID_PROPERTY_NAME).getAsString(), type);
			userVarItem.fromJsonObject(element.getAsJsonObject());

			UserVarComponent userVarComponent = userVarItem.getUserVarComponent();

			userVarComponent.deleteUserVarValue(index);

			return true;
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public void setDefaultPlayerVarValues() {

		// set all 'bit' type player variables with default index 0 set to value
		// default.

		for (var entry : helper.entrySet()) {
			var entryObj = entry.getValue().getAsJsonObject();
			if (entryObj.get("type").getAsInt() == UserVarType.Bit.val) {
				var defId = Integer.parseInt(entry.getKey());

				if (this.getPlayerVarValue(defId, 0) == null) {
					var defaultVal = entryObj.get("defaultValue").getAsInt();
					this.setPlayerVarValue(defId, 0, defaultVal);

					Centuria.logger.debug(MarkerManager.getMarker("USERVARS"),
							"Setting uservar " + entryObj.get("userVarName").getAsString() + " (DefId: " + defId
									+ ") to default value of " + defaultVal + ".");
				}
			}
		}

	}

}
