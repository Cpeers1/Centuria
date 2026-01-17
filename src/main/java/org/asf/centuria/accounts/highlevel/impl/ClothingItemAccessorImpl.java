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
	public boolean hasClothing(String defID) {
		return inventory.getAccessor().hasInventoryObjectByDefId("100", defID);
	}

	@Override
	public int getClothingCount(String defID) {
		int count = 0;

		// Load the inventory object
		if (!inventory.containsItem("100"))
			inventory.setItem("100", new JsonArray());
		JsonArray items = inventory.getItem("100").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			String itID = itm.get("defId").getAsString();
			if (itID.equals(defID)) {
				count++;
			}
		}

		return count;
	}

	@Override
	public void removeClothing(String id) {
		inventory.getAccessor().removeInventoryObjectByItemId("100", id);
	}

	@Override
	public JsonObject getClothingData(String id) {
		return inventory.getAccessor().findInventoryObjectByItemId("100", id);
	}

	@Override
	public String addClothing(String defID, boolean isInTradeList) {
		String cID = null;

		// Generate object
		// Check existence
		if (helper.has(defID)) {
			// Trade thingy
			JsonObject tr = new JsonObject();
			tr.addProperty("isInTradeList", isInTradeList);

			// Add item
			cID = inventory.getAccessor().createInventoryObject("100", defID, new ItemComponent("Tradable", tr),
					new ItemComponent("Colorable", helper.get(defID).getAsJsonObject()));
		}

		// Return ID
		return cID;
	}

	@Override
	public JsonObject getDefaultClothingChannelHSV(String defID, int channel) {
		// Check existence
		if (helper.has(defID)) {
			// Find channel
			JsonObject data = helper.get(defID).getAsJsonObject();
			if (data.has("color" + channel + "HSV"))
				return data.get("color" + channel + "HSV").getAsJsonObject();
		}
		return null;
	}

}
