package org.asf.centuria.accounts.highlevel.itemdata.inventory.impl;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.highlevel.itemdata.inventory.AbstractInventoryInteractionHelper;
import org.asf.centuria.entities.inventoryitems.InventoryItem;

import com.google.gson.JsonObject;

public class FurnitureHelper extends AbstractInventoryInteractionHelper {

	private static final String INV_TYPE = "102";

	@Override
	public JsonObject addOne(PlayerInventory inventory, String defID) {
		String id = inventory.getFurnitureAccessor().addFurniture(defID, false);
		return inventory.getAccessor().findInventoryObjectByItemId(INV_TYPE, id);
	}

	@Override
	public JsonObject addOne(PlayerInventory inventory, JsonObject object) {
		if (inventory.getAccessor().hasInventoryObjectByItemId(INV_TYPE,
				object.get(InventoryItem.UUID_PROPERTY_NAME).getAsString())) {
			// Remove existing
			inventory.getAccessor().removeInventoryObjectByItemId(INV_TYPE,
					object.get(InventoryItem.UUID_PROPERTY_NAME).getAsString());
		}

		// Add to inventory
		inventory.getItem(INV_TYPE).getAsJsonArray().add(object);

		// Return object
		return object;
	}

	@Override
	public String removeOne(PlayerInventory inventory, String defID) {
		if (inventory.getFurnitureAccessor().hasFurniture(defID)) {
			String uuid = inventory.getAccessor().findInventoryObjectByDefId(INV_TYPE, defID)
					.get(InventoryItem.UUID_PROPERTY_NAME).getAsString();
			inventory.getAccessor().removeInventoryObjectByItemId(INV_TYPE, uuid);
			return uuid;
		}
		return null;
	}

	@Override
	public String removeOne(PlayerInventory inventory, JsonObject object) {
		if (inventory.getFurnitureAccessor()
				.hasFurniture(object.get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsString())) {
			String uuid = object.get(InventoryItem.UUID_PROPERTY_NAME).getAsString();
			inventory.getFurnitureAccessor().removeFurniture(uuid);
			return uuid;
		}
		return null;
	}

}
