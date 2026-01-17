package org.asf.centuria.accounts.highlevel.itemdata.inventory.impl;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.highlevel.itemdata.inventory.AbstractInventoryInteractionHelper;
import org.asf.centuria.entities.inventoryitems.InventoryItem;

import com.google.gson.JsonObject;

public class AvatarSpeciesHelper extends AbstractInventoryInteractionHelper {

	private static final String INV_TYPE = "1";

	@Override
	public JsonObject addOne(PlayerInventory inventory, String defID) {
		inventory.getAvatarAccessor().unlockAvatarSpecies(defID);
		return inventory.getAccessor().findInventoryObjectByDefId(INV_TYPE, defID);
	}

	@Override
	public JsonObject addOne(PlayerInventory inventory, JsonObject object) {
		return addOne(inventory, object.get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsString());
	}

	@Override
	public String removeOne(PlayerInventory inventory, String defID) {
		if (inventory.getAvatarAccessor().isAvatarSpeciesUnlocked(defID)) {
			String uuid = inventory.getAccessor().findInventoryObjectByDefId(INV_TYPE, defID)
					.get(InventoryItem.UUID_PROPERTY_NAME).getAsString();
			inventory.getAccessor().removeInventoryObjectByItemId(INV_TYPE, uuid);
			return uuid;
		}
		return null;
	}

	@Override
	public String removeOne(PlayerInventory inventory, JsonObject object) {
		return removeOne(inventory, object.get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsString());
	}

}
