package org.asf.centuria.accounts.highlevel.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.highlevel.SanctuaryAccessor;
import org.asf.centuria.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.centuria.entities.components.generic.TimeStampComponent;
import org.asf.centuria.entities.inventoryitems.InventoryItem;
import org.asf.centuria.entities.sanctuaries.RoomInfoObject;
import org.asf.centuria.entities.sanctuaries.SanctuaryObjectData;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

public class SanctuaryAccessorImpl extends SanctuaryAccessor {
	private static JsonObject helper;

	private static final int FALLBACK_ITEM_LIMIT = 250;

	static {
		// Load helper
		InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
				.getResourceAsStream("defaultitems/sanctuaryclasseshelper.json");
		try {
			helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("SanctuaryClasses").getAsJsonObject();
			strm.close();
		} catch (JsonSyntaxException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public SanctuaryAccessorImpl(PlayerInventory inventory) {
		super(inventory);
	}

	@Override
	public int[] getUnlockedIslandTypes() {
		ArrayList<Integer> types = new ArrayList<Integer>();

		// Load the inventory object
		if (!inventory.containsItem("6"))
			inventory.setItem("6", new JsonArray());
		JsonArray items = inventory.getItem("6").getAsJsonArray();

		// Find all IDs
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();

			// Add ID
			int itID = itm.get("defId").getAsInt();
			if (!types.contains(itID))
				types.add(itID);
		}

		// Return the type IDs
		int[] typeIds = new int[types.size()];
		for (int i = 0; i < typeIds.length; i++)
			typeIds[i] = types.get(i);
		return typeIds;
	}

	@Override
	public int getIslandTypeItemCount(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("6"))
			inventory.setItem("6", new JsonArray());
		JsonArray items = inventory.getItem("6").getAsJsonArray();
		int count = 0;

		// Find island
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();

			// Check ID
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				count++;
			}
		}

		// Not found
		return count;
	}

	@Override
	public String addIslandToInventory(int defID) {
		// Build island info object
		JsonObject island = new JsonObject();
		island.addProperty("gridId", 0);
		island.addProperty("themeDefId", 0);

		// Create item
		return inventory.getAccessor().createInventoryObject("6", defID, new ItemComponent("Island", island));
	}

	@Override
	public JsonObject getIslandTypeObject(String id) {
		return inventory.getAccessor().findInventoryObject("6", id);
	}

	@Override
	public boolean isIslandTypeUnlocked(int defID) {
		return inventory.getAccessor().hasInventoryObject("6", defID);
	}

	@Override
	public int[] getUnlockedHouseTypes() {
		ArrayList<Integer> types = new ArrayList<Integer>();

		// Load the inventory object
		if (!inventory.containsItem("5"))
			inventory.setItem("5", new JsonArray());
		JsonArray items = inventory.getItem("5").getAsJsonArray();

		// Find all IDs
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();

			// Add ID
			int itID = itm.get("defId").getAsInt();
			if (!types.contains(itID))
				types.add(itID);
		}

		// Return the type IDs
		int[] typeIds = new int[types.size()];
		for (int i = 0; i < typeIds.length; i++)
			typeIds[i] = types.get(i);
		return typeIds;
	}

	@Override
	public int getHouseTypeItemCount(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("5"))
			inventory.setItem("5", new JsonArray());
		JsonArray items = inventory.getItem("5").getAsJsonArray();
		int count = 0;

		// Find house
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();

			// Check ID
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				count++;
			}
		}

		// Not found
		return count;
	}

	@Override
	public String addHouseToInventory(int defID) {
		// Build house info object
		JsonObject house = new JsonObject();
		house.addProperty("stage", 0);
		house.add("roomData", new JsonArray());
		house.addProperty("x", 0);
		house.addProperty("y", 0);
		house.addProperty("gridId", 0);
		house.addProperty("themeDefId", 0);
		JsonArray enlargedAreas = new JsonArray();
		for (int i = 0; i < 10; i++) {
			enlargedAreas.add(0);
		}
		house.add("enlargedAreas", enlargedAreas);

		// Create item
		return inventory.getAccessor().createInventoryObject("5", defID, new ItemComponent("House", house));
	}

	@Override
	public JsonObject getHouseTypeObject(String id) {
		return inventory.getAccessor().findInventoryObject("5", id);
	}

	@Override
	public boolean isHouseTypeUnlocked(int defID) {
		return inventory.getAccessor().hasInventoryObject("5", defID);
	}

	@Override
	public int[] getUnlockedSanctuaries() {
		ArrayList<Integer> types = new ArrayList<Integer>();

		// Load the inventory object
		if (!inventory.containsItem("10"))
			inventory.setItem("10", new JsonArray());
		JsonArray items = inventory.getItem("10").getAsJsonArray();

		// Find all IDs
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();

			// Add ID
			int itID = itm.get("defId").getAsInt();
			if (!types.contains(itID))
				types.add(itID);
		}

		// Return the type IDs
		int[] typeIds = new int[types.size()];
		for (int i = 0; i < typeIds.length; i++)
			typeIds[i] = types.get(i);
		return typeIds;
	}

	@Override
	public void unlockSanctuary(int defID) {
		// Check unlocked sanctuary
		if (isSanctuaryUnlocked(defID))
			return;

		// Load the inventory object
		if (!inventory.containsItem("10"))
			inventory.setItem("10", new JsonArray());
		JsonArray items = inventory.getItem("10").getAsJsonArray();

		// Generate item ID
		String itmID = UUID.randomUUID().toString();
		while (true) {
			boolean found = false;
			for (JsonElement ele : items) {
				JsonObject itm = ele.getAsJsonObject();
				String itID = itm.get("id").getAsString();
				if (itID.equals(itmID)) {
					found = true;
					break;
				}
			}
			if (!found)
				break;
			itmID = UUID.randomUUID().toString();
		}

		// Create the object
		JsonObject itm = new JsonObject();
		JsonObject ts = new JsonObject();
		ts.addProperty("ts", System.currentTimeMillis());
		// Build class info object
		JsonObject classI = new JsonObject();
		classI.addProperty("stage", 0);
		JsonArray enlargedAreas = new JsonArray();
		for (int i = 0; i < 10; i++) {
			enlargedAreas.add(0);
		}
		classI.add("enlargedAreas", enlargedAreas);
		JsonObject components = new JsonObject();
		components.add("SanctuaryClass", classI);
		components.add("Timestamp", ts);
		itm.addProperty("defId", defID);
		itm.add("components", components);
		itm.addProperty("id", itmID);
		itm.addProperty("type", 10);
		items.add(itm);

		// Load class info
		JsonObject classData = helper.get(Integer.toString(defID)).getAsJsonObject();
		int islandId = classData.get("islandDefId").getAsInt();
		int houseId = classData.get("houseDefId").getAsInt();
		int lookDefId = classData.get("lookDefId").getAsInt();

		// Create look slots
		if (!inventory.containsItem("201"))
			inventory.setItem("201", new JsonArray());
		items = inventory.getItem("201").getAsJsonArray();
		for (int i = 0; i < getSanctuaryLookCount(); i++) {
			// Generate item ID
			itmID = UUID.randomUUID().toString();
			while (true) {
				boolean found = false;
				for (JsonElement ele : items) {
					itm = ele.getAsJsonObject();
					String itID = itm.get("id").getAsString();
					if (itID.equals(itmID)) {
						found = true;
						break;
					}
				}
				if (!found)
					break;
				itmID = UUID.randomUUID().toString();
			}

			// Add slot
			createSlot(items, islandId, houseId, lookDefId, defID, itmID);
		}

		//
		// Create primary slot
		//

		// Generate item ID
		itmID = UUID.randomUUID().toString();
		while (true) {
			boolean found = false;
			for (JsonElement ele : items) {
				itm = ele.getAsJsonObject();
				String itID = itm.get("id").getAsString();
				if (itID.equals(itmID)) {
					found = true;
					break;
				}
			}
			if (!found)
				break;
			itmID = UUID.randomUUID().toString();
		}

		// Add slot
		createSlot(items, islandId, houseId, lookDefId, defID, itmID).get("components").getAsJsonObject()
				.add("PrimaryLook", new JsonObject());

		// Mark what files to save
		addItemToSave("10");
		addItemToSave("201");
	}

	@Override
	public JsonObject getSanctuaryClassObject(String id) {
		return inventory.getAccessor().findInventoryObject("10", id);
	}

	@Override
	public boolean isSanctuaryUnlocked(int defID) {
		return inventory.getAccessor().hasInventoryObject("10", defID);
	}

	@Override
	public int getSanctuaryLookCount() {
		// Load the inventory object
		if (!inventory.containsItem("201"))
			return 0;

		// Find first primary look
		for (JsonElement ele : inventory.getItem("201").getAsJsonArray()) {
			if (ele.getAsJsonObject().get("components").getAsJsonObject().has("PrimaryLook")) {
				// Count for this look
				int count = 0;

				// Loop through all looks and find matches
				for (JsonElement ele2 : inventory.getItem("201").getAsJsonArray()) {
					if (!ele2.getAsJsonObject().get("components").getAsJsonObject().has("PrimaryLook")
							&& ele2.getAsJsonObject().get("defId").getAsInt() == ele.getAsJsonObject().get("defId")
									.getAsInt()) {
						count++;
					}
				}

				return count;
			}
		}

		// No match
		return 0;
	}

	@Override
	public JsonObject getSanctuaryLook(String lookID) {
		return inventory.getAccessor().findInventoryObject("201", lookID);
	}

	@Override
	public JsonObject getFirstSanctuaryLook() {
		// Load the inventory object
		if (!inventory.containsItem("201"))
			inventory.setItem("201", new JsonArray());
		JsonArray items = inventory.getItem("201").getAsJsonArray();

		// Find sanctuary
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			if (itm.get("components").getAsJsonObject().has("PrimaryLook"))
				return itm;
		}

		// Could not find one
		return null;
	}

	@Override
	public String[] getSanctuaryLookIDs() {
		// Load the inventory object
		if (!inventory.containsItem("201"))
			inventory.setItem("201", new JsonArray());
		JsonArray items = inventory.getItem("201").getAsJsonArray();

		// Build list
		ArrayList<String> sancs = new ArrayList<String>();

		// Find sanctuaries
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			sancs.add(itm.get("id").getAsString());
		}

		// Could not find one
		return sancs.toArray(t -> new String[t]);
	}

	@Override
	public void addExtraSanctuarySlot() {
		// Load the inventory object
		if (!inventory.containsItem("201"))
			inventory.setItem("201", new JsonArray());
		JsonArray items = inventory.getItem("201").getAsJsonArray();

		// Add for each sanctuary class
		for (int classId : getUnlockedSanctuaries()) {
			// Load ids
			JsonObject classData = helper.get(Integer.toString(classId)).getAsJsonObject();
			int islandId = classData.get("islandDefId").getAsInt();
			int houseId = classData.get("houseDefId").getAsInt();
			int lookDefId = classData.get("lookDefId").getAsInt();

			// Generate item ID
			String itmID = UUID.randomUUID().toString();
			while (true) {
				boolean found = false;
				for (JsonElement ele : items) {
					JsonObject itm = ele.getAsJsonObject();
					String itID = itm.get("id").getAsString();
					if (itID.equals(itmID)) {
						found = true;
						break;
					}
				}
				if (!found)
					break;
				itmID = UUID.randomUUID().toString();
			}

			// Create slot
			createSlot(items, islandId, houseId, lookDefId, classId, itmID);

			// Mark what files to save
			addItemToSave("201");
		}
	}

	@Override
	public boolean addSanctuaryObject(String objectUUID, SanctuaryObjectData sancObjectInfo, String activeSancLookId) {
		// get the object def id from the funiture inv
		int defId = inventory.getFurnitureAccessor().getDefIDFromUUID(objectUUID);

		// find sanc look
		if (!inventory.containsItem("201"))
			inventory.setItem("201", new JsonArray());

		var looks = inventory.getItem("201").getAsJsonArray();

		JsonElement sancLook = null;

		for (var item : looks) {
			if (item.getAsJsonObject().get("id").getAsString().equals(activeSancLookId)) {
				sancLook = item;
				break;
			}
		}

		if (sancLook == null)
			return false; // cannot find

		var placementInfo = sancLook.getAsJsonObject().get("components").getAsJsonObject().get("SanctuaryLook")
				.getAsJsonObject().get("info").getAsJsonObject().get("placementInfo").getAsJsonObject();

		// check if items array exists

		JsonArray itemsArray = new JsonArray();
		boolean itemsExisted = false;

		if (placementInfo.has("items")) {
			itemsArray = placementInfo.get("items").getAsJsonArray();
			itemsExisted = true;
		}

		// check if item exists

		JsonObject item = null;
		int index = 0;

		for (var itemEle : itemsArray) {
			if (itemEle.getAsJsonObject().get("id").getAsString().equals(objectUUID)) {
				item = itemEle.getAsJsonObject();
				break;
			}
			index++;
		}

		if (item != null) {
			// just remove the object first, before adding an element for it back in
			// this is probably faster then trying to change it
			itemsArray.remove(index);
		} else {
			// adding a new item
			// check if we hit item limit

			// update as with client modding we can increase the limit, load it from the
			// player data if overridden
			int limit = FALLBACK_ITEM_LIMIT;
			if (inventory.getSaveSettings().sanctuaryLimitOverride != -1)
				limit = inventory.getSaveSettings().sanctuaryLimitOverride;

			if (itemsArray.size() + 1 > limit) {
				// can't add this object
				return false;
			}
		}

		// construct a new item object

		item = new JsonObject();
		item.addProperty("defId", defId);

		JsonObject componentLevel = new JsonObject();
		JsonObject placedLevel = new JsonObject();

		placedLevel.addProperty("xPos", sancObjectInfo.positionInfo.position.x);
		placedLevel.addProperty("yPos", sancObjectInfo.positionInfo.position.y);
		placedLevel.addProperty("zPos", sancObjectInfo.positionInfo.position.z);

		placedLevel.addProperty("rotX", sancObjectInfo.positionInfo.rotation.x);
		placedLevel.addProperty("rotY", sancObjectInfo.positionInfo.rotation.y);
		placedLevel.addProperty("rotZ", sancObjectInfo.positionInfo.rotation.z);
		placedLevel.addProperty("rotW", sancObjectInfo.positionInfo.rotation.w);

		placedLevel.addProperty("parentItemId", ""); // ?
		placedLevel.addProperty("placeableInvId", objectUUID);

		placedLevel.addProperty("gridId", sancObjectInfo.gridId);
		placedLevel.addProperty("state", sancObjectInfo.state);

		componentLevel.add("Placed", placedLevel);
		item.add("components", componentLevel);
		item.addProperty("id", objectUUID);
		item.addProperty("type", 102);

		itemsArray.add(item);

		if (!itemsExisted) {
			placementInfo.add("items", itemsArray);
		}

		inventory.setItem("201", looks);
		return true;
	}

	@Override
	public void removeSanctuaryObject(String objectUUID, String activeSancLookId) {
		// find sanc look
		if (!inventory.containsItem("201"))
			inventory.setItem("201", new JsonArray());

		var looks = inventory.getItem("201").getAsJsonArray();

		JsonElement sancLook = null;

		for (var item : looks) {
			if (item.getAsJsonObject().get("id").getAsString().equals(activeSancLookId)) {
				sancLook = item;
				break;
			}
		}

		if (sancLook == null)
			return; // cannot find

		var placementInfo = sancLook.getAsJsonObject().get("components").getAsJsonObject().get("SanctuaryLook")
				.getAsJsonObject().get("info").getAsJsonObject().get("placementInfo").getAsJsonObject();

		// check if items array exists
		if (!placementInfo.has("items")) {
			// yeah.. I can't do jack
			return;
		}

		var itemsArray = placementInfo.get("items").getAsJsonArray();

		// now to find the element with the uuid

		JsonElement foundObject = null;
		int index = 0;

		for (var item : itemsArray) {
			if (item.getAsJsonObject().get("id").getAsString().equals(objectUUID)) {
				foundObject = item;
				break;
			}

			index++;
		}

		if (foundObject == null)
			return;

		itemsArray.remove(index);

		// removed from the items array
		inventory.setItem("201", looks);
	}

	@Override
	public JsonObject updateSanctuaryRoomData(String activeSancLookId, RoomInfoObject[] roomInfos) {
		// find sanc look
		if (!inventory.containsItem("201"))
			inventory.setItem("201", new JsonArray());

		var looks = inventory.getItem("201").getAsJsonArray();

		JsonElement sancLook = null;

		for (var item : looks) {
			if (item.getAsJsonObject().get("id").getAsString().equals(activeSancLookId)) {
				sancLook = item;
				break;
			}
		}

		if (sancLook == null)
			return null; // cannot find

		var info = sancLook.getAsJsonObject().get("components").getAsJsonObject().get("SanctuaryLook").getAsJsonObject()
				.get("info").getAsJsonObject();

		// from here, we can get the house inv ID

		var houseInvId = info.get("houseInvId").getAsString();

		// then, we can use that to find the house inv ID in inv 5

		if (!inventory.containsItem("5"))
			inventory.setItem("5", new JsonArray());

		var houseInvs = inventory.getItem("5").getAsJsonArray();

		JsonElement houseInv = null;

		for (var item : houseInvs) {
			if (item.getAsJsonObject().get("id").getAsString().equals(houseInvId)) {
				houseInv = item;
				break;
			}
		}

		if (houseInv == null)
			return null; // cannot find

		// okay, we got the house inventory object
		// the next part is updating the room data
		// for every roomInfo passed in..

		JsonArray roomInfoLevel = houseInv.getAsJsonObject().get("components").getAsJsonObject().get("House")
				.getAsJsonObject().get("roomData").getAsJsonArray();

		// its actually an array of strings that have json objects in them.. WW why

		for (var roomInfo : roomInfos) {
			// check if roomInfoLevel has an entry with this room index already.

			int index = 0;
			boolean found = false;

			for (var item2 : roomInfoLevel) {
				var object = JsonParser.parseString(item2.getAsString());

				if (object.getAsJsonObject().get("roomIndex").getAsInt() == roomInfo.roomIndex) {
					found = true;
					break;
				}

				index++;
			}

			if (found) {
				// remove the old room data, we will repopulate it
				roomInfoLevel.remove(index);
			}

			// create a new room info object
			// add the json object to the array
			var room = roomInfo.toJson().toString();
			roomInfoLevel.add(room);
		}

		// set house inventory again
		inventory.setItem("5", houseInvs);

		return houseInv.getAsJsonObject();
	}

	@Override
	public void saveSanctuaryLookToSlot(String activeSancLookId, String slotId, String saveName) {

		// find sanc look
		if (!inventory.containsItem("201"))
			inventory.setItem("201", new JsonArray());

		var looks = inventory.getItem("201").getAsJsonArray();

		JsonElement oldSancSlot = null;

		for (var item : looks) {
			if (item.getAsJsonObject().get("id").getAsString().equals(activeSancLookId)) {
				oldSancSlot = item;
				break;
			}
		}

		if (oldSancSlot == null)
			return; // cannot find

		// now, find the slot (if it doesn't exist, make it

		JsonElement newSancSlot = null;

		for (var item : looks) {
			if (item.getAsJsonObject().get("id").getAsString().equals(slotId)) {
				newSancSlot = item;
				break;
			}
		}

		if (newSancSlot == null)
			return; // cannot find

		// we're basically going to copy this look into the slot with ID of slotId
		// important: we're only copying the SanctuaryLook component.

		var oldInfoComponent = oldSancSlot.getAsJsonObject().get("components").getAsJsonObject().get("SanctuaryLook")
				.getAsJsonObject().get("info").getAsJsonObject();

		var newInfoComponent = newSancSlot.getAsJsonObject().get("components").getAsJsonObject().get("SanctuaryLook")
				.getAsJsonObject().get("info").getAsJsonObject();

		// get all the id's we will need for the transfer
		var oldHouseInvId = oldInfoComponent.get("houseInvId").getAsString();
		var oldIslandInvId = oldInfoComponent.get("islandInvId").getAsString();
		var oldClassInvId = oldInfoComponent.get("classInvId").getAsString();

		var newHouseInvId = newInfoComponent.get("houseInvId").getAsString();
		var newIslandInvId = newInfoComponent.get("islandInvId").getAsString();
		var newClassInvId = newInfoComponent.get("classInvId").getAsString();

		// timestamp NOW
		var timeStamp = System.currentTimeMillis();

		copySancLookInv(looks, newSancSlot, oldSancSlot, saveName, timeStamp);
		copyHouseInv(oldHouseInvId, newHouseInvId, timeStamp);
		copyIslandInv(oldIslandInvId, newIslandInvId, timeStamp);
		copyClassInv(oldClassInvId, newClassInvId, timeStamp);

		// hopefully that worked.
	}

	private void copyClassInv(String oldClassInvId, String newClassInvId, long timeStamp) {
		// find classInv
		if (!inventory.containsItem("10"))
			inventory.setItem("10", new JsonArray());

		var classInv = inventory.getItem("10").getAsJsonArray();

		// copy house inv to the new island inv
		// first find the old island inv

		JsonElement oldClassInv = null;

		for (var item : classInv) {
			if (item.getAsJsonObject().get("id").getAsString().equals(oldClassInvId)) {
				oldClassInv = item;
				break;
			}
		}

		if (oldClassInv == null)
			return; // cannot find

		// now, find the slot (if it doesn't exist, make it

		JsonElement newClassInv = null;

		for (var item : classInv) {
			if (item.getAsJsonObject().get("id").getAsString().equals(newClassInvId)) {
				newClassInv = item;
				break;
			}
		}

		if (newClassInv == null)
			return; // cannot find

		// copy old island inv to new house inv

		var oldComponentLevel = oldClassInv.getAsJsonObject().get("components").getAsJsonObject();

		var classLevel = oldComponentLevel.get("SanctuaryClass");

		// deep copy it
		var classCopy = classLevel.deepCopy();

		// replace island copy
		var newComponentLevel = newClassInv.getAsJsonObject().get("components").getAsJsonObject();

		// remove the old island data
		newComponentLevel.remove("SanctuaryClass");

		// add the new island data
		newComponentLevel.add("SanctuaryClass", classCopy);

		// update timeStamp

		var tsLevel = newComponentLevel.get("Timestamp").getAsJsonObject();

		tsLevel.remove("ts");
		tsLevel.addProperty("ts", timeStamp);

		// save the 10
		inventory.setItem("10", classInv);
	}

	private void copyIslandInv(String oldIslandInvId, String newIslandInvId, long timeStamp) {
		// find islandInvId
		if (!inventory.containsItem("6"))
			inventory.setItem("6", new JsonArray());

		var islandInv = inventory.getItem("6").getAsJsonArray();

		// copy house inv to the new island inv
		// first find the old island inv

		JsonElement oldIslandInv = null;

		for (var item : islandInv) {
			if (item.getAsJsonObject().get("id").getAsString().equals(oldIslandInvId)) {
				oldIslandInv = item;
				break;
			}
		}

		if (oldIslandInv == null)
			return; // cannot find

		// now, find the slot (if it doesn't exist, make it

		JsonElement newIslandInv = null;

		for (var item : islandInv) {
			if (item.getAsJsonObject().get("id").getAsString().equals(newIslandInvId)) {
				newIslandInv = item;
				break;
			}
		}

		if (newIslandInv == null)
			return; // cannot find

		// copy old island inv to new house inv

		var oldComponentLevel = oldIslandInv.getAsJsonObject().get("components").getAsJsonObject();

		var islandLevel = oldComponentLevel.get("Island");

		// deep copy it
		var islandCopy = islandLevel.deepCopy();

		// replace island copy
		var newComponentLevel = newIslandInv.getAsJsonObject().get("components").getAsJsonObject();

		// remove the old island data
		newComponentLevel.remove("Island");

		// add the new island data
		newComponentLevel.add("Island", islandCopy);

		// update timeStamp

		var tsLevel = newComponentLevel.get("Timestamp").getAsJsonObject();

		tsLevel.remove("ts");
		tsLevel.addProperty("ts", timeStamp);

		// save the 6
		inventory.setItem("6", islandInv);
	}

	private void copyHouseInv(String oldHouseInvId, String newHouseInvId, long timeStamp) {
		// find houseInvId
		if (!inventory.containsItem("5"))
			inventory.setItem("5", new JsonArray());

		var houseInv = inventory.getItem("5").getAsJsonArray();

		// copy house inv to the new house inv
		// first find the old house inv

		JsonElement oldHouseInv = null;

		for (var item : houseInv) {
			if (item.getAsJsonObject().get("id").getAsString().equals(oldHouseInvId)) {
				oldHouseInv = item;
				break;
			}
		}

		if (oldHouseInv == null)
			return; // cannot find

		// now, find the slot (if it doesn't exist, make it

		JsonElement newHouseInv = null;

		for (var item : houseInv) {
			if (item.getAsJsonObject().get("id").getAsString().equals(newHouseInvId)) {
				newHouseInv = item;
				break;
			}
		}

		if (newHouseInv == null)
			return; // cannot find

		// copy old house inv to new house inv

		var oldComponentLevel = oldHouseInv.getAsJsonObject().get("components").getAsJsonObject();

		var houseLevel = oldComponentLevel.get("House");

		// deep copy it
		var houseCopy = houseLevel.deepCopy();

		// replace house copy
		var newComponentLevel = newHouseInv.getAsJsonObject().get("components").getAsJsonObject();

		// remove the old house data
		newComponentLevel.remove("House");

		// add the new house data
		newComponentLevel.add("House", houseCopy);

		// update timeStamp

		var tsLevel = newComponentLevel.get("Timestamp").getAsJsonObject();

		tsLevel.remove("ts");
		tsLevel.addProperty("ts", timeStamp);

		// save the 5
		inventory.setItem("5", houseInv);
	}

	private void copySancLookInv(JsonArray looks, JsonElement newSancSlot, JsonElement oldSancSlot, String saveName,
			long timeStamp) {

		var oldComponentLevel = oldSancSlot.getAsJsonObject().get("components").getAsJsonObject();

		var oldInfoComponent = oldComponentLevel.get("SanctuaryLook").getAsJsonObject().get("info").getAsJsonObject();

		var newComponentLevel = newSancSlot.getAsJsonObject().get("components").getAsJsonObject();

		var newInfoComponent = newComponentLevel.get("SanctuaryLook").getAsJsonObject().get("info").getAsJsonObject();

		var oldPlacementsComponent = oldInfoComponent.get("placementInfo").getAsJsonObject();

		// ok, we should make a copy of this
		var newPlacementComponent = oldPlacementsComponent.deepCopy();

		newInfoComponent.remove("placementInfo");

		// and add the copy of the sanc look we made instead..

		newInfoComponent.add("placementInfo", newPlacementComponent);

		// update TS ...
		var tsLevel = newComponentLevel.get("Timestamp").getAsJsonObject();

		tsLevel.remove("ts");
		tsLevel.addProperty("ts", timeStamp);

		// add a name component...

		JsonObject newNameLevel = new JsonObject();
		newNameLevel.addProperty("name", saveName);

		if (newComponentLevel.has("Name")) {
			newComponentLevel.remove("Name");
		}

		newComponentLevel.add("Name", newNameLevel);

		// save the 201
		inventory.setItem("201", looks);
	}

	private JsonObject createSlot(JsonArray items, int islandId, int houseId, int lookDefId, int classId,
			String itmID) {
		// Build object
		JsonObject itm = new JsonObject();
		JsonObject ts = new JsonObject();
		ts.addProperty("ts", System.currentTimeMillis());
		// Build sanctuary info object
		JsonObject sanctuary = new JsonObject();
		JsonObject sanctuaryInfo = new JsonObject();
		// Create the house
		String id = addHouseToInventory(houseId);
		sanctuaryInfo.addProperty("houseDefId", houseId);
		sanctuaryInfo.addProperty("houseInvId", id);
		sanctuaryInfo.add("placementInfo", new JsonObject());
		// Create the island
		id = addIslandToInventory(islandId);
		sanctuaryInfo.addProperty("islandDefId", islandId);
		sanctuaryInfo.addProperty("islandInvId", id);
		// Find class
		id = inventory.getAccessor().findInventoryObject("10", classId).get("id").getAsString();
		sanctuaryInfo.addProperty("classInvId", id);
		sanctuary.add("info", sanctuaryInfo);
		// Name object
		JsonObject nm = new JsonObject();
		nm.addProperty("name", "");
		// Build components
		JsonObject components = new JsonObject();
		// Make primary if needed
		components.add("SanctuaryLook", sanctuary);
		components.add("Name", nm);
		components.add("Timestamp", ts);
		itm.addProperty("defId", lookDefId);
		itm.add("components", components);
		itm.addProperty("id", itmID);
		itm.addProperty("type", 201);
		items.add(itm);
		return itm;
	}

	@Override
	public boolean upgradeSanctuaryToStage(String sancClassInvId, int stage) {
		// Need to upgrade sanctuary..
		if (!inventory.containsItem("10"))
			inventory.setItem("10", new JsonArray());

		var classInv = inventory.getItem("10").getAsJsonArray();

		JsonElement classObject = null;

		for (var item : classInv) {
			if (item.getAsJsonObject().get("id").getAsString().equals(sancClassInvId)) {
				classObject = item;
				break;
			}
		}

		if (classObject == null)
			return false;

		// ok..
		// we can update the stage on this
		var sancClass = classObject.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME).getAsJsonObject()
				.get("SanctuaryClass").getAsJsonObject();

		sancClass.remove("stage");
		sancClass.addProperty("stage", stage);

		JsonObject ts = new JsonObject();
		ts.addProperty("ts", System.currentTimeMillis());

		classObject.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME).getAsJsonObject()
				.remove(TimeStampComponent.COMPONENT_NAME);

		classObject.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME).getAsJsonObject()
				.add(TimeStampComponent.COMPONENT_NAME, ts);

		// we also need to update the house and island invs of any looks using this
		// class

		List<JsonObject> lookObjectsToUpdate = new ArrayList<JsonObject>();

		if (!inventory.containsItem("201"))
			inventory.setItem("201", new JsonArray());

		var looks = inventory.getItem("201").getAsJsonArray();

		for (var item : looks) {
			var infoLevel = item.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME).getAsJsonObject()
					.get("SanctuaryLook").getAsJsonObject().get("info").getAsJsonObject();

			if (infoLevel.get("classInvId").getAsString().equals(sancClassInvId)) {
				lookObjectsToUpdate.add(item.getAsJsonObject());
			}
		}

		// now that we found all the looks to update, we need to update their house
		// inventories..

		if (!inventory.containsItem("5"))
			inventory.setItem("5", new JsonArray());

		var houseInv = inventory.getItem("5").getAsJsonArray();

		for (var item : lookObjectsToUpdate) {
			var infoLevel = item.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME).getAsJsonObject()
					.get("SanctuaryLook").getAsJsonObject().get("info").getAsJsonObject();

			var houseInvId = infoLevel.get("houseInvId").getAsString();

			JsonElement matchedHouseItem = null;

			for (var houseItem : houseInv) {
				if (houseItem.getAsJsonObject().get("id").getAsString().equals(houseInvId)) {
					matchedHouseItem = houseItem;
					break;
				}
			}

			if (matchedHouseItem != null) {
				var houseLevel = matchedHouseItem.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME)
						.getAsJsonObject().get("House").getAsJsonObject();

				// update stage
				houseLevel.remove("stage");
				houseLevel.addProperty("stage", stage);

				// stamp
				matchedHouseItem.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME).getAsJsonObject()
						.remove("ts");
				matchedHouseItem.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME).getAsJsonObject()
						.add("ts", ts);
			} else {
				return false;
			}

			// stamp
			item.remove("ts");
			item.add("ts", ts);
		}

		// save the 10
		inventory.setItem("10", classInv);

		// save the 5
		inventory.setItem("5", houseInv);

		// save the 201
		inventory.setItem("201", looks);

		return true;
	}

	@Override
	public boolean enlargenSanctuaryRooms(String sancClassInvId, int roomIndex) {

		// Need to upgrade sanctuary..
		if (!inventory.containsItem("10"))
			inventory.setItem("10", new JsonArray());

		var classInv = inventory.getItem("10").getAsJsonArray();

		JsonElement classObject = null;

		for (var item : classInv) {
			if (item.getAsJsonObject().get("id").getAsString().equals(sancClassInvId)) {
				classObject = item;
				break;
			}
		}

		if (classObject == null)
			return false;

		// ok..
		// we can update the room enlarge array on this
		var sancClass = classObject.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME).getAsJsonObject()
				.get("SanctuaryClass").getAsJsonObject();

		var roomEnlargeArray = sancClass.get("enlargedAreas").getAsJsonArray();
		roomEnlargeArray.set(roomIndex, new JsonPrimitive(1));

		JsonObject ts = new JsonObject();
		ts.addProperty("ts", System.currentTimeMillis());

		classObject.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME).getAsJsonObject()
				.remove(TimeStampComponent.COMPONENT_NAME);

		classObject.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME).getAsJsonObject()
				.add(TimeStampComponent.COMPONENT_NAME, ts);

		// we also need to update the house and island invs of any looks using this
		// class

		List<JsonObject> lookObjectsToUpdate = new ArrayList<JsonObject>();

		if (!inventory.containsItem("201"))
			inventory.setItem("201", new JsonArray());

		var looks = inventory.getItem("201").getAsJsonArray();

		for (var item : looks) {
			var infoLevel = item.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME).getAsJsonObject()
					.get("SanctuaryLook").getAsJsonObject().get("info").getAsJsonObject();

			if (infoLevel.get("classInvId").getAsString().equals(sancClassInvId)) {
				lookObjectsToUpdate.add(item.getAsJsonObject());
			}
		}

		// now that we found all the looks to update, we need to update their house
		// inventories..

		if (!inventory.containsItem("5"))
			inventory.setItem("5", new JsonArray());

		var houseInv = inventory.getItem("5").getAsJsonArray();

		for (var item : lookObjectsToUpdate) {
			var infoLevel = item.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME).getAsJsonObject()
					.get("SanctuaryLook").getAsJsonObject().get("info").getAsJsonObject();

			var houseInvId = infoLevel.get("houseInvId").getAsString();

			JsonElement matchedHouseItem = null;

			for (var houseItem : houseInv) {
				if (houseItem.getAsJsonObject().get("id").getAsString().equals(houseInvId)) {
					matchedHouseItem = houseItem;
					break;
				}
			}

			if (matchedHouseItem != null) {
				var houseLevel = matchedHouseItem.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME)
						.getAsJsonObject().get("House").getAsJsonObject();

				// update room enlarge array
				roomEnlargeArray = houseLevel.get("enlargedAreas").getAsJsonArray();
				roomEnlargeArray.set(roomIndex, new JsonPrimitive(1));

				// stamp
				matchedHouseItem.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME).getAsJsonObject()
						.remove("ts");
				matchedHouseItem.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME).getAsJsonObject()
						.add("ts", ts);
			} else {
				return false;
			}

			// stamp
			item.remove("ts");
			item.add("ts", ts);
		}

		// save the 10
		inventory.setItem("10", classInv);

		// save the 5
		inventory.setItem("5", houseInv);

		// save the 201
		inventory.setItem("201", looks);

		return true;
	}

	@Override
	public int getCurrentSanctuaryStage(String sancClassInvId) {

		if (!inventory.containsItem("10"))
			inventory.setItem("10", new JsonArray());

		var classInv = inventory.getItem("10").getAsJsonArray();

		JsonElement classObject = null;

		for (var item : classInv) {
			if (item.getAsJsonObject().get("id").getAsString().equals(sancClassInvId)) {
				classObject = item;
				break;
			}
		}

		if (classObject == null)
			return 0;

		return classObject.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME).getAsJsonObject()
				.get("SanctuaryClass").getAsJsonObject().get("stage").getAsInt();
	}

	@Override
	public JsonArray getExpandedRooms(String sancClassInvId) {

		if (!inventory.containsItem("10"))
			inventory.setItem("10", new JsonArray());

		var classInv = inventory.getItem("10").getAsJsonArray();

		JsonElement classObject = null;

		for (var item : classInv) {
			if (item.getAsJsonObject().get("id").getAsString().equals(sancClassInvId)) {
				classObject = item;
				break;
			}
		}

		if (classObject == null)
			return null;

		return classObject.getAsJsonObject().get(InventoryItem.COMPONENTS_PROPERTY_NAME).getAsJsonObject()
				.get("SanctuaryClass").getAsJsonObject().get("enlargedAreas").getAsJsonArray();
	}

}