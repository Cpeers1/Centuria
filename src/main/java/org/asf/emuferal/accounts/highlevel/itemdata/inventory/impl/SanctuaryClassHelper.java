package org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.AbstractInventoryInteractionHelper;

import com.google.gson.JsonObject;

public class SanctuaryClassHelper extends AbstractInventoryInteractionHelper {

	@Override
	public JsonObject addOne(PlayerInventory inventory, int defID) {
		if (inventory.getSanctuaryAccessor().isSanctuaryUnlocked(defID))
			return null;
		inventory.getSanctuaryAccessor().unlockSanctuary(defID);
		return inventory.getAccessor().findInventoryObject("10", defID);
	}

	@Override
	public JsonObject addOne(PlayerInventory inventory, JsonObject object) {
		return addOne(inventory, object.get("defId").getAsInt());
	}

	@Override
	public boolean removeOne(PlayerInventory inventory, int defID) {
		if (inventory.getSanctuaryAccessor().isSanctuaryUnlocked(defID)) {
			inventory.getAccessor().removeInventoryObject("10", defID);
			return true;
		}
		return false;
	}

	@Override
	public boolean removeOne(PlayerInventory inventory, JsonObject object) {
		return removeOne(inventory, object.get("defId").getAsInt());
	}

}
