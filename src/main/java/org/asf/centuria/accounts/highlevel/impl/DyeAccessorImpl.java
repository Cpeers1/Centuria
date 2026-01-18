package org.asf.centuria.accounts.highlevel.impl;

import java.io.InputStream;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.highlevel.DyeAccessor;
import org.asf.centuria.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DyeAccessorImpl extends DyeAccessor {
	private static JsonObject helper;

	static {
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/dyehelper.json");
			helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject().get("Dyes")
					.getAsJsonObject();
			strm.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public DyeAccessorImpl(PlayerInventory inventory) {
		super(inventory);
	}

	@Override
	public String addDyeByDefId(String defID) {
		// Find dye
		JsonObject dye = getDyeDataByDefID(defID);

		// Add one to the quantity field
		int q = dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().get("quantity").getAsInt();
		dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().remove("quantity");
		dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().addProperty("quantity", q + 1);

		// Mark what files to save
		markChanged("111", dye.get("id").getAsString());

		// Return ID
		return dye.get("id").getAsString();
	}

	@Override
	public void removeDyeByDefId(String defID) {
		// Find dye
		JsonObject dye = getDyeDataByDefID(defID);

		// Remove one to the quantity field
		int q = dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().get("quantity").getAsInt();
		dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().remove("quantity");
		dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().addProperty("quantity", q - 1);

		if (q - 1 <= 0) {
			// Remove object
			inventory.getItem("111").getAsJsonArray().remove(dye);
		}

		// Mark what files to save
		markDeleted("111", dye.get("id").getAsString());
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

				// Update
				markChanged("111", dye.get("id").getAsString());
				if (q - 1 <= 0) {
					// Remove object
					inventory.getItem("111").getAsJsonArray().remove(dye);
					markDeleted("111", dye.get("id").getAsString());
				}

				// End loop
				break;
			}
		}
	}

	@Override
	public JsonObject getDyeData(String id) {
		return inventory.getAccessor().findInventoryObjectByItemId("111", id);
	}

	@Override
	public String getDyeHSV(String defID) {
		if (helper.has(defID))
			return helper.get(defID).getAsString();
		return null;
	}

	@Override
	public boolean hasDye(String defID) {
		return inventory.getAccessor().hasInventoryObjectByDefId("111", defID);
	}

	// Retrieves information objects for dyes and makes it if not present
	private JsonObject getDyeDataByDefID(String defID) {
		//
		// Find object
		//

		if (inventory.getAccessor().hasInventoryObjectByDefId("111", defID))
			return inventory.getAccessor().findInventoryObjectByDefId("111", defID);

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
		return inventory.getAccessor().findInventoryObjectByItemId("111", itId);
	}

}
