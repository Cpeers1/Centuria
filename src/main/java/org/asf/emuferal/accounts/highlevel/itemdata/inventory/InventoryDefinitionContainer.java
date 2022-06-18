package org.asf.emuferal.accounts.highlevel.itemdata.inventory;

public class InventoryDefinitionContainer {

	public InventoryType inventoryType;
	public AbstractInventoryInteractionHelper inventoryInteraction;

	public InventoryDefinitionContainer(InventoryType type, AbstractInventoryInteractionHelper helper) {
		inventoryType = type;
		inventoryInteraction = helper;
	}

}
