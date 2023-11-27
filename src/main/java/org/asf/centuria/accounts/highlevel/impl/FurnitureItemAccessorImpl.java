package org.asf.centuria.accounts.highlevel.impl;

import java.io.InputStream;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.highlevel.FurnitureItemAccessor;
import org.asf.centuria.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FurnitureItemAccessorImpl extends FurnitureItemAccessor {
	private static JsonObject helper;
	static {
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("content/items/furniture.json");
			helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject().get("Furniture")
					.getAsJsonObject();
			strm.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public FurnitureItemAccessorImpl(PlayerInventory inventory) {
		super(inventory);
	}

	@Override
	public boolean hasFurniture(int defID) {
		return inventory.getAccessor().hasInventoryObject("102", defID);
	}

	@Override
	public int getFurnitureCount(int defID) {
		int count = 0;

		// Load the inventory object
		if (!inventory.containsItem("102"))
			inventory.setItem("102", new JsonArray());
		JsonArray items = inventory.getItem("102").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				count++;
			}
		}

		return count;
	}

	@Override
	public void removeFurniture(String id) {
		inventory.getAccessor().removeInventoryObject("102", id);
	}

	@Override
	public JsonObject getFurnitureData(String id) {
		return inventory.getAccessor().findInventoryObject("102", id);
	}

	@Override
	public String addFurniture(int defID, boolean isInTradeList) {
		String cID = null;

		// Generate object

		// Check existence
		if (helper.has(Integer.toString(defID))) {
			// Trade thingy
			JsonObject tr = new JsonObject();
			tr.addProperty("isInTradeList", isInTradeList);

			// Add item
			cID = inventory.getAccessor().createInventoryObject("102", defID, new ItemComponent("Tradable", tr),
					new ItemComponent("Colorable", helper.get(Integer.toString(defID)).getAsJsonObject()),
					new ItemComponent("Placeable", new JsonObject()));
		}

		// Return ID
		return cID;
	}

	@Override
	public JsonObject getDefaultFurnitureChannelHSV(int defID, int channel) {
		// Check existence
		if (helper.has(Integer.toString(defID))) {
			// Find channel
			JsonObject data = helper.get(Integer.toString(defID)).getAsJsonObject();
			if (data.has("color" + channel + "HSV"))
				return data.get("color" + channel + "HSV").getAsJsonObject();
		}
		return null;
	}

	@Override
	public int getDefIDFromUUID(String placeableUUID) {

		// unfortunately we have to loop through objects to find it..

		var object = inventory.getAccessor().findInventoryObject("102", placeableUUID);

		return object.get("defId").getAsInt();
	}

}
