package org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.AbstractInventoryInteractionHelper;
import org.asf.emuferal.entities.inventoryitems.InventoryItem;

import com.google.gson.JsonObject;

public class SanctuaryClassHelper extends AbstractInventoryInteractionHelper {

	private static final String INV_TYPE = "10";

	@Override
	public JsonObject addOne(PlayerInventory inventory, int defID) {
		if (inventory.getSanctuaryAccessor().isSanctuaryUnlocked(defID))
			return null;
		inventory.getSanctuaryAccessor().unlockSanctuary(defID);
		return inventory.getAccessor().findInventoryObject(INV_TYPE, defID);
	}

	@Override
	public JsonObject addOne(PlayerInventory inventory, JsonObject object) {
		return addOne(inventory, object.get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsInt());
	}

	@Override
	public String removeOne(PlayerInventory inventory, int defID) {
		if (inventory.getSanctuaryAccessor().isSanctuaryUnlocked(defID)) {
			String uuid = inventory.getAccessor().findInventoryObject(INV_TYPE, defID)
					.get(InventoryItem.UUID_PROPERTY_NAME).getAsString();
			inventory.getAccessor().removeInventoryObject(INV_TYPE, defID);
			return uuid;
		}
		return null;
	}

	@Override
	public String removeOne(PlayerInventory inventory, JsonObject object) {
		return removeOne(inventory, object.get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsInt());
	}

}
