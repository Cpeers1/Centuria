package org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.AbstractInventoryInteractionHelper;

import com.google.gson.JsonObject;

public class CurrencyHelper extends AbstractInventoryInteractionHelper {

	@Override
	public JsonObject addOne(PlayerInventory inventory, int defID) {
		return add(inventory, defID, 1);
	}

	@Override
	public JsonObject[] addMultiple(PlayerInventory inventory, int defID, int count) {
		JsonObject obj = add(inventory, defID, count);
		if (obj != null)
			return new JsonObject[] { obj };
		return new JsonObject[0];
	}

	private JsonObject add(PlayerInventory inventory, int defID, int count) {
		// Find type
		switch (defID) {

		// Likes
		case 2327:
			inventory.getCurrencyAccessor().addLikesDirectly(count);
			return inventory.getAccessor().findInventoryObject("104", defID);

		// Star fragments
		case 14500:
			inventory.getCurrencyAccessor().addStarFragmentsDirectly(count);
			return inventory.getAccessor().findInventoryObject("104", defID);

		// Lockpicks
		case 8372:
			inventory.getCurrencyAccessor().addLockpicksDirectly(count);
			return inventory.getAccessor().findInventoryObject("104", defID);

		}

		// Not found
		return null;
	}

	private boolean remove(PlayerInventory inventory, int defID, int count) {
		// Find type
		switch (defID) {

		// Likes
		case 2327:
			return inventory.getCurrencyAccessor().removeLikesDirectly(count);

		// Star fragments
		case 14500:
			return inventory.getCurrencyAccessor().removeStarFragmentsDirectly(count);

		// Lockpicks
		case 8372:
			return inventory.getCurrencyAccessor().removeLockpicksDirectly(count);

		}

		// Not found
		return false;
	}

	@Override
	public JsonObject addOne(PlayerInventory inventory, JsonObject object) {
		return addOne(inventory, object.get("defId").getAsInt());
	}

	@Override
	public boolean removeMultiple(PlayerInventory inventory, int defID, int count) {
		return remove(inventory, defID, count);
	}

	@Override
	public boolean removeOne(PlayerInventory inventory, int defID) {
		return remove(inventory, defID, 1);
	}

	@Override
	public boolean removeOne(PlayerInventory inventory, JsonObject object) {
		return removeOne(inventory, object.get("defId").getAsInt());
	}

}
