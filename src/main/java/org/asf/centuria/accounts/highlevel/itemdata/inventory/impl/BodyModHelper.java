package org.asf.centuria.accounts.highlevel.itemdata.inventory.impl;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.highlevel.itemdata.inventory.AbstractInventoryInteractionHelper;
import org.asf.centuria.entities.inventoryitems.InventoryItem;

import com.google.gson.JsonObject;

public class BodyModHelper extends AbstractInventoryInteractionHelper {

	private static final String INV_TYPE = "2";
	
	@Override
	public JsonObject addOne(PlayerInventory inventory, int defID) {
		inventory.getAvatarAccessor().unlockAvatarPart(Integer.toString(defID)); // FIXME
		return inventory.getAccessor().findInventoryObject(INV_TYPE, defID);
	}

	@Override
	public JsonObject addOne(PlayerInventory inventory, JsonObject object) {
		return addOne(inventory, object.get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsInt());
	}

	@Override
	public String removeOne(PlayerInventory inventory, int defID) {
		if (inventory.getAvatarAccessor().isAvatarPartUnlocked(Integer.toString(defID))) { // FIXME
			String uuid = inventory.getAccessor().findInventoryObject(INV_TYPE, defID).get(InventoryItem.UUID_PROPERTY_NAME).getAsString();
			inventory.getAvatarAccessor().lockAvatarPart(Integer.toString(defID)); // FIXME
			return uuid;
		}
		return null;
	}

	@Override
	public String removeOne(PlayerInventory inventory, JsonObject object) {
		return removeOne(inventory, object.get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsInt());
	}

}
