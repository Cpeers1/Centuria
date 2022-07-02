package org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.AbstractInventoryInteractionHelper;
import org.asf.emuferal.entities.inventoryitems.InventoryItem;

import com.google.gson.JsonObject;

public class CurrencyHelper extends AbstractInventoryInteractionHelper {

	private static final String INV_TYPE = "104";

	private static final int LIKES_DEF_ID = 2327;
	private static final int STAR_FRAGMENTS_DEF_ID = 14500;
	private static final int LOCKPICKS_DEF_ID = 8372;

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
		case LIKES_DEF_ID:
			inventory.getCurrencyAccessor().addLikesDirectly(count);
			return inventory.getAccessor().findInventoryObject(INV_TYPE, defID);

		// Star fragments
		case STAR_FRAGMENTS_DEF_ID:
			inventory.getCurrencyAccessor().addStarFragmentsDirectly(count);
			return inventory.getAccessor().findInventoryObject(INV_TYPE, defID);

		// Lockpicks
		case LOCKPICKS_DEF_ID:
			inventory.getCurrencyAccessor().addLockpicksDirectly(count);
			return inventory.getAccessor().findInventoryObject(INV_TYPE, defID);

		}

		// Not found
		return null;
	}

	private String remove(PlayerInventory inventory, int defID, int count) {

		var uuid = inventory.getAccessor().findInventoryObject(INV_TYPE, defID).get(InventoryItem.UUID_PROPERTY_NAME)
				.getAsString();
		
		// Find type
		switch (defID) {

		// Likes
		case LIKES_DEF_ID:
			inventory.getCurrencyAccessor().removeLikesDirectly(count);
			return uuid;

		// Star fragments
		case STAR_FRAGMENTS_DEF_ID:
			inventory.getCurrencyAccessor().removeStarFragmentsDirectly(count);
			return uuid;

		// Lockpicks
		case LOCKPICKS_DEF_ID:
			inventory.getCurrencyAccessor().removeLockpicksDirectly(count);
			return uuid;

		}

		// Not found
		return null;
	}

	@Override
	public JsonObject addOne(PlayerInventory inventory, JsonObject object) {
		return addOne(inventory, object.get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsInt());
	}

	@Override
	public String[] removeMultiple(PlayerInventory inventory, int defID, int count) {		
		return new String[] { remove(inventory, defID, count) };
	}

	@Override
	public String removeOne(PlayerInventory inventory, int defID) {
		return remove(inventory, defID, 1);
	}

	@Override
	public String removeOne(PlayerInventory inventory, JsonObject object) {
		return removeOne(inventory, object.get(InventoryItem.DEF_ID_PROPERTY_NAME).getAsInt());
	}

}
