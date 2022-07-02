package org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.AbstractInventoryInteractionHelper;
import org.asf.emuferal.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.emuferal.entities.inventoryitems.InventoryItem;

import com.google.gson.JsonObject;

public class GenericHelper extends AbstractInventoryInteractionHelper {
	
	private String inventoryId;
	private ItemComponent[] components;

	public GenericHelper(String inventoryId, ItemComponent... components) {
		this.inventoryId = inventoryId;
		this.components = components;
	}

	@Override
	public JsonObject addOne(PlayerInventory inventory, int defID) {
		String id = inventory.getAccessor().createInventoryObject(inventoryId, defID, components);
		if (id == null)
			return null;
		return inventory.getAccessor().findInventoryObject(inventoryId, id);
	}

	@Override
	public JsonObject addOne(PlayerInventory inventory, JsonObject object) {
		if (!inventory.getAccessor().hasInventoryObject(inventoryId, object.get("id").getAsString())) {
			// Add the item directly
			inventory.getItem(inventoryId).getAsJsonArray().add(object);
			inventory.setItem(inventoryId, inventory.getItem(inventoryId));
			return object;
		}
		return addOne(inventory, object.get("defId").getAsInt());
	}

	@Override
	public String removeOne(PlayerInventory inventory, int defID) {
		if (inventory.getAccessor().hasInventoryObject(inventoryId, defID)) {
			String uuid = inventory.getAccessor().findInventoryObject(inventoryId, defID)
					.get(InventoryItem.UUID_PROPERTY_NAME).getAsString();
			inventory.getAccessor().removeInventoryObject(inventoryId, defID);
			return uuid;
		}
		
		return null;
	}

	@Override
	public String removeOne(PlayerInventory inventory, JsonObject object) {
		if (inventory.getAccessor().hasInventoryObject(inventoryId, object.get(InventoryItem.UUID_PROPERTY_NAME).getAsString())) {
			// Remove the item directly
			String uuid = inventory.getAccessor().findInventoryObject(inventoryId, InventoryItem.UUID_PROPERTY_NAME)
					.get(InventoryItem.UUID_PROPERTY_NAME).getAsString();
			inventory.getItem(inventoryId).getAsJsonArray().remove(object);
			inventory.setItem(inventoryId, inventory.getItem(inventoryId));
			return uuid;
		}
		return removeOne(inventory, object.get("defId").getAsInt());
	}

}
