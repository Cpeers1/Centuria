package org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.AbstractInventoryInteractionHelper;

import com.google.gson.JsonObject;

public class GenericHelper extends AbstractInventoryInteractionHelper {

	private String inventoryId;

	public GenericHelper(String inventoryId) {
		this.inventoryId = inventoryId;
	}

	@Override
	public JsonObject addOne(PlayerInventory inventory, int defID) {
		String id = inventory.getAccessor().createInventoryObject(inventoryId, defID);
		if (id == null)
			return null;
		return inventory.getAccessor().findInventoryObject(inventoryId, id);
	}

	@Override
	public JsonObject addOne(PlayerInventory inventory, JsonObject object) {
		return addOne(inventory, object.get("defId").getAsInt());
	}

	@Override
	public boolean removeOne(PlayerInventory inventory, int defID) {
		if (inventory.getAccessor().hasInventoryObject(inventoryId, defID)) {
			inventory.getAccessor().removeInventoryObject(inventoryId, defID);
			return true;
		}
		return false;
	}

	@Override
	public boolean removeOne(PlayerInventory inventory, JsonObject object) {
		return removeOne(inventory, object.get("defId").getAsInt());
	}

}
