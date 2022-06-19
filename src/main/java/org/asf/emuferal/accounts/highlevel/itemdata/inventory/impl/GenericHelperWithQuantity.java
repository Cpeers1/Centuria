package org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.AbstractInventoryInteractionHelper;
import org.asf.emuferal.accounts.highlevel.itemdata.item.ItemComponent;

import com.google.gson.JsonObject;

public class GenericHelperWithQuantity extends AbstractInventoryInteractionHelper {

	private String inventoryId;
	private ItemComponent[] components;

	public GenericHelperWithQuantity(String inventoryId, ItemComponent... components) {
		this.inventoryId = inventoryId;
		this.components = components;
	}

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
		// Find object
		JsonObject old = inventory.getAccessor().findInventoryObject(inventoryId, defID);
		if (old == null) {
			// Create new instance

			// Quantity
			JsonObject qt = new JsonObject();
			qt.addProperty("quantity", 0);

			// Add item
			ItemComponent[] components = new ItemComponent[this.components.length + 1];
			for (int i = 0; i < this.components.length; i++)
				components[i] = this.components[i];
			components[this.components.length] = new ItemComponent("Quantity", qt);
			old = inventory.getAccessor().findInventoryObject(inventoryId,
					inventory.getAccessor().createInventoryObject(inventoryId, defID, components));
		}

		// Add to the quantity
		JsonObject qt = old.get("components").getAsJsonObject().get("Quantity").getAsJsonObject();
		int oldQuantity = qt.get("quantity").getAsInt();
		qt.remove("quantity");
		qt.addProperty("quantity", oldQuantity + count);

		// Return object
		return old;
	}

	private boolean remove(PlayerInventory inventory, int defID, int count) {
		// Find object
		JsonObject old = inventory.getAccessor().findInventoryObject(inventoryId, defID);
		if (old == null) {
			// Not found
			return false;
		}

		// Load quantity
		JsonObject qt = old.get("components").getAsJsonObject().get("Quantity").getAsJsonObject();
		int oldQuantity = qt.get("quantity").getAsInt();

		// Check validity
		if ((oldQuantity - count) <= 0) {
			// Remove object
			inventory.getAccessor().removeInventoryObject(inventoryId, defID);
			return true;
		}

		// Remove from quantity
		qt.remove("quantity");
		qt.addProperty("quantity", oldQuantity - count);

		// Success
		return true;
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
