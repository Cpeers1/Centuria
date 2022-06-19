package org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.AbstractInventoryInteractionHelper;

import com.google.gson.JsonObject;

public class FurnitureHelper extends AbstractInventoryInteractionHelper {

	@Override
	public JsonObject addOne(PlayerInventory inventory, int defID) {
		String id = inventory.getFurnitureAccessor().addFurniture(defID, false);
		return inventory.getAccessor().findInventoryObject("102", id);
	}

	@Override
	public JsonObject addOne(PlayerInventory inventory, JsonObject object) {
		if (inventory.getAccessor().hasInventoryObject("102", object.get("id").getAsString())) {
			// Remove existing
			inventory.getAccessor().removeInventoryObject("102", object.get("id").getAsString());

			// Add to inventory
			inventory.getItem("102").getAsJsonArray().add(object);

			// Return object
			return object;
		}
		return addOne(inventory, object.get("defId").getAsInt());
	}

	@Override
	public boolean removeOne(PlayerInventory inventory, int defID) {
		if (inventory.getFurnitureAccessor().hasFurniture(defID)) {
			inventory.getAccessor().removeInventoryObject("102", defID);
			return true;
		}
		return false;
	}

	@Override
	public boolean removeOne(PlayerInventory inventory, JsonObject object) {
		if (inventory.getFurnitureAccessor().hasFurniture(object.get("defId").getAsInt())) {
			inventory.getFurnitureAccessor().removeFurniture(object.get("id").getAsString());
			return true;
		}
		return false;
	}

}
