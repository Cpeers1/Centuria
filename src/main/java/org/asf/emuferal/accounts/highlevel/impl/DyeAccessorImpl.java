package org.asf.emuferal.accounts.highlevel.impl;

import java.io.IOException;
import java.io.InputStream;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.DyeAccessor;
import org.asf.emuferal.accounts.highlevel.itemdata.ItemComponent;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DyeAccessorImpl extends DyeAccessor {
	public DyeAccessorImpl(PlayerInventory inventory) {
		super(inventory);
	}

	@Override
	public String addDye(int defID) {
		// Find dye
		JsonObject dye = getDyeData(defID);

		// Add one to the quantity field
		int q = dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().get("quantity").getAsInt();
		dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().remove("quantity");
		dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().addProperty("quantity", q + 1);

		// Mark what files to save
		addItemToSave("111");

		// Return ID
		return dye.get("id").getAsString();
	}

	@Override
	public void removeDye(int defID) {
		// Find dye
		JsonObject dye = getDyeData(defID);

		// Remove one to the quantity field
		int q = dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().get("quantity").getAsInt();
		dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().remove("quantity");
		dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().addProperty("quantity", q - 1);

		if (q - 1 <= 0) {
			// Remove object
			inventory.getItem("111").getAsJsonArray().remove(dye);
		}

		// Mark what files to save
		addItemToSave("111");
	}

	@Override
	public void removeDye(String id) {
		// Load the inventory object
		if (!inventory.containsItem("111"))
			inventory.setItem("111", new JsonArray());
		JsonArray items = inventory.getItem("111").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject dye = ele.getAsJsonObject();
			String itID = dye.get("id").getAsString();
			if (itID.equals(id)) {
				// Remove one to the quantity field
				int q = dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().get("quantity")
						.getAsInt();
				dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().remove("quantity");
				dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().addProperty("quantity",
						q - 1);

				if (q - 1 <= 0) {
					// Remove object
					inventory.getItem("111").getAsJsonArray().remove(dye);
				}

				// Mark what files to save
				addItemToSave("111");

				// End loop
				break;
			}
		}
	}

	@Override
	public JsonObject getDyeData(String id) {
		return inventory.getAccessor().findInventoryObject("111", id);
	}

	@Override
	public String getDyeHSV(int defID) {
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/dyehelper.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Dyes").getAsJsonObject();
			strm.close();

			if (helper.has(Integer.toString(defID)))
				return helper.get(Integer.toString(defID)).getAsString();
		} catch (IOException e) {
		}

		return null;
	}

	@Override
	public boolean hasDye(int defID) {
		return inventory.getAccessor().hasInventoryObject("111", defID);
	}

	// Retrieves information objects for dyes and makes it if not present
	private JsonObject getDyeData(int defID) {
		//
		// Find object
		//

		if (inventory.getAccessor().hasInventoryObject("111", defID))
			return inventory.getAccessor().findInventoryObject("111", defID);

		//
		// Add the item
		//

		// Trade thingy
		JsonObject tr = new JsonObject();
		tr.addProperty("isInTradeList", false);

		// Quantity
		JsonObject qt = new JsonObject();
		qt.addProperty("quantity", 0);

		// Add item
		String itId = inventory.getAccessor().createInventoryObject("111", defID, new ItemComponent("Tradable", tr),
				new ItemComponent("Quantity", qt));

		return inventory.getAccessor().findInventoryObject("111", itId);
	}

}
