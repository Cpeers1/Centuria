package org.asf.centuria.accounts.highlevel.impl;

import java.io.InputStream;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.highlevel.ClothingItemAccessor;
import org.asf.centuria.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ClothingItemAccessorImpl extends ClothingItemAccessor {
	private static JsonObject helper;
	static {
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/clothinghelper.json");
			helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject().get("Clothing")
					.getAsJsonObject();
			strm.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public ClothingItemAccessorImpl(PlayerInventory inventory) {
		super(inventory);
	}

	@Override
	public boolean hasClothing(int defID) {
		return inventory.getAccessor().hasInventoryObject("100", defID);
	}

	@Override
	public int getClothingCount(int defID) {
		int count = 0;

		// Load the inventory object
		if (!inventory.containsItem("100"))
			inventory.setItem("100", new JsonArray());
		JsonArray items = inventory.getItem("100").getAsJsonArray();

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
	public void removeClothing(String id) {
		inventory.getAccessor().removeInventoryObject("100", id);
	}

	@Override
	public JsonObject getClothingData(String id) {
		return inventory.getAccessor().findInventoryObject("100", id);
	}

	@Override
	public String addClothing(int defID, boolean isInTradeList) {
		String cID = null;

		// Generate object
		// Check existence
		if (helper.has(Integer.toString(defID))) {
			// Trade thingy
			JsonObject tr = new JsonObject();
			tr.addProperty("isInTradeList", isInTradeList);

			// Add item
			cID = inventory.getAccessor().createInventoryObject("100", defID, new ItemComponent("Tradable", tr),
					new ItemComponent("Colorable", helper.get(Integer.toString(defID)).getAsJsonObject()));
		}

		// Return ID
		return cID;
	}

	@Override
	public String getDefaultClothingChannelHSV(int defID, int channel) {
		// Check existence
		if (helper.has(Integer.toString(defID))) {
			// Find channel
			JsonObject data = helper.get(Integer.toString(defID)).getAsJsonObject();
			if (data.has("color" + channel + "HSV"))
				return data.get("color" + channel + "HSV").getAsJsonObject().get("_hsv").getAsString();
		}
		return null;
	}

}
