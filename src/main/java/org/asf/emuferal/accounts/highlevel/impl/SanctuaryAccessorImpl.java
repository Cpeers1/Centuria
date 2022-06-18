package org.asf.emuferal.accounts.highlevel.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.SanctuaryAccessor;
import org.asf.emuferal.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class SanctuaryAccessorImpl extends SanctuaryAccessor {
	private static JsonObject helper;
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
		// Build components
		JsonObject components = new JsonObject();
		// Make primary if needed
		components.add("SanctuaryLook", sanctuary);
		components.add("Timestamp", ts);
		itm.addProperty("defId", lookDefId);
		itm.add("components", components);
		itm.addProperty("id", itmID);
		itm.addProperty("type", 201);
		items.add(itm);
		return itm;
	}
}
