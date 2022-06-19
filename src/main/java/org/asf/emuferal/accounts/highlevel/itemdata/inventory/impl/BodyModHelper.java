package org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.AbstractInventoryInteractionHelper;

import com.google.gson.JsonObject;

public class BodyModHelper extends AbstractInventoryInteractionHelper {

	@Override
	public JsonObject addOne(PlayerInventory inventory, int defID) {
		inventory.getAvatarAccessor().unlockAvatarPart(defID);
		return inventory.getAccessor().findInventoryObject("2", defID);
	}

	@Override
	public JsonObject addOne(PlayerInventory inventory, JsonObject object) {
		return addOne(inventory, object.get("defId").getAsInt());
	}

	@Override
	public boolean removeOne(PlayerInventory inventory, int defID) {
		if (inventory.getAvatarAccessor().isAvatarPartUnlocked(defID)) {
			inventory.getAvatarAccessor().lockAvatarPart(defID);
			return true;
		}
		return false;
	}

	@Override
	public boolean removeOne(PlayerInventory inventory, JsonObject object) {
		return removeOne(inventory, object.get("defId").getAsInt());
	}

}
