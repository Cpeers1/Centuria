package org.asf.centuria.accounts.highlevel.itemdata.inventory.impl;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.highlevel.itemdata.inventory.AbstractInventoryInteractionHelper;
import org.asf.centuria.entities.inventoryitems.InventoryItem;

import com.google.gson.JsonObject;

public class FurnitureHelper extends AbstractInventoryInteractionHelper {

	private static final String INV_TYPE = "102";

	@Override
	public JsonObject addOne(PlayerInventory inventory, int defID) {
		String id = inventory.getFurnitureAccessor().addFurniture(defID, false);
		return inventory.getAccessor().findInventoryObject(INV_TYPE, id);
	}

	@Override
	public JsonObject addOne(PlayerInventory inventory, JsonObject object) {
		if (inventory.getAccessor().hasInventoryObject(INV_TYPE,
				object.get(InventoryItem.UUID_PROPERTY_NAME).getAsString())) {
			// Remove existing
			inventory.getAccessor().removeInventoryObject(INV_TYPE,
					object.get(InventoryItem.UUID_PROPERTY_NAME).getAsString());

			// Add to inventory
			inventory.getItem(INV_TYPE).getAsJsonArray().add(object);

			// Return object
			return object;
		}
		return addOne(inventory, object.get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsInt());
	}

	@Override
	public String removeOne(PlayerInventory inventory, int defID) {
		if (inventory.getFurnitureAccessor().hasFurniture(defID)) {
			String uuid = inventory.getAccessor().findInventoryObject(INV_TYPE, defID)
					.get(InventoryItem.UUID_PROPERTY_NAME).getAsString();
			inventory.getAccessor().removeInventoryObject(INV_TYPE, defID);
			return uuid;
		}
		return null;
	}

	@Override
	public String removeOne(PlayerInventory inventory, JsonObject object) {
		if (inventory.getFurnitureAccessor().hasFurniture(object.get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsInt())) {
			String uuid = object.get(InventoryItem.UUID_PROPERTY_NAME).getAsString();
			inventory.getFurnitureAccessor().removeFurniture(uuid);
			return uuid;
		}
		return null;
	}

}
