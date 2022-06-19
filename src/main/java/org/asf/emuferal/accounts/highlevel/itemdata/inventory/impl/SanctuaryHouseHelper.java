package org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.AbstractInventoryInteractionHelper;

import com.google.gson.JsonObject;

public class SanctuaryHouseHelper extends AbstractInventoryInteractionHelper {

	@Override
	public JsonObject addOne(PlayerInventory inventory, int defID) {
		String id = inventory.getSanctuaryAccessor().addHouseToInventory(defID);
		return inventory.getAccessor().findInventoryObject("5", id);
	}

	@Override
	public JsonObject addOne(PlayerInventory inventory, JsonObject object) {
		if (inventory.getAccessor().hasInventoryObject("5", object.get("id").getAsString())) {
			// Remove existing
			inventory.getAccessor().removeInventoryObject("5", object.get("id").getAsString());

			// Add to inventory
			inventory.getItem("5").getAsJsonArray().add(object);

			// Return object
			return object;
		}
		return addOne(inventory, object.get("defId").getAsInt());
	}

	@Override
	public boolean removeOne(PlayerInventory inventory, int defID) {
		if (inventory.getSanctuaryAccessor().isHouseTypeUnlocked(defID)) {
			inventory.getAccessor().removeInventoryObject("5", defID);
			return true;
		}
		return false;
	}

	@Override
	public boolean removeOne(PlayerInventory inventory, JsonObject object) {
		if (inventory.getSanctuaryAccessor().isHouseTypeUnlocked(object.get("defId").getAsInt())) {
			inventory.getAccessor().removeInventoryObject("5", object.get("id").getAsString());
			return true;
		}
		return false;
	}

}
