package org.asf.centuria.accounts.highlevel.itemdata.inventory;

import org.asf.centuria.enums.inventory.InventoryStorageType;

public class InventoryDefinitionContainer {

	public InventoryStorageType inventoryType;
	public AbstractInventoryInteractionHelper inventoryInteraction;

	public InventoryDefinitionContainer(InventoryStorageType type, AbstractInventoryInteractionHelper helper) {
		inventoryType = type;
		inventoryInteraction = helper;
	}

}
