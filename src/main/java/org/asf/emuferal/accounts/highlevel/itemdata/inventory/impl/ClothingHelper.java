package org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.AbstractInventoryInteractionHelper;

import com.google.gson.JsonObject;

public class ClothingHelper extends AbstractInventoryInteractionHelper {

	@Override
	public JsonObject addOne(PlayerInventory inventory, int defID) {
		String id = inventory.getClothingAccessor().addClothing(defID, false);
		return inventory.getAccessor().findInventoryObject("100", id);
	}

	@Override
	public JsonObject addOne(PlayerInventory inventory, JsonObject object) {
		if (inventory.getAccessor().hasInventoryObject("100", object.get("id").getAsString())) {
			// Remove existing
			inventory.getAccessor().removeInventoryObject("100", object.get("id").getAsString());

			// Add to inventory
			inventory.getItem("100").getAsJsonArray().add(object);

			// Return object
			return object;
		}
		return addOne(inventory, object.get("defId").getAsInt());
	}

	@Override
	public boolean removeOne(PlayerInventory inventory, int defID) {
		if (inventory.getClothingAccessor().hasClothing(defID)) {
			inventory.getAccessor().removeInventoryObject("100", defID);
			return true;
		}
		return false;
	}

	@Override
	public boolean removeOne(PlayerInventory inventory, JsonObject object) {
		if (inventory.getClothingAccessor().hasClothing(object.get("defId").getAsInt())) {
			inventory.getClothingAccessor().removeClothing(object.get("id").getAsString());
			return true;
		}
		return false;
	}

}
