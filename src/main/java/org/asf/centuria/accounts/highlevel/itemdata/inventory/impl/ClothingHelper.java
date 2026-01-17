package org.asf.centuria.accounts.highlevel.itemdata.inventory.impl;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.highlevel.itemdata.inventory.AbstractInventoryInteractionHelper;
import org.asf.centuria.entities.inventoryitems.InventoryItem;

import com.google.gson.JsonObject;

public class ClothingHelper extends AbstractInventoryInteractionHelper {

	private static final String INV_TYPE = "100";

	@Override
	public JsonObject addOne(PlayerInventory inventory, String defID) {
		String id = inventory.getClothingAccessor().addClothing(defID, false);
		if (id == null)
			return null;
		return inventory.getAccessor().findInventoryObjectByDefId(INV_TYPE, id);
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
		if (inventory.getClothingAccessor().hasClothing(defID)) {
			String uuid = inventory.getAccessor().findInventoryObjectByDefId(INV_TYPE, defID)
					.get(InventoryItem.UUID_PROPERTY_NAME).getAsString();
			inventory.getAccessor().removeInventoryObjectByDefId(INV_TYPE, defID);

			return uuid;
		}

		return null;
	}

	@Override
	public String removeOne(PlayerInventory inventory, JsonObject object) {
		if (inventory.getClothingAccessor().hasClothing(object.get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsString())) {
			String uuid = object.get(InventoryItem.UUID_PROPERTY_NAME).getAsString();
			inventory.getClothingAccessor().removeClothing(uuid);
			return uuid;
		}

		return null;
	}

}
