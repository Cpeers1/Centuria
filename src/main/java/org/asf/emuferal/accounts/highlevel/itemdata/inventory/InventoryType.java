package org.asf.emuferal.accounts.highlevel.itemdata.inventory;

public enum InventoryType {

	/**
	 * Object-based, but only accepts one item
	 */
	SINGLE_ITEM,

	/**
	 * Quantity-based inventory (uses a quantity field)
	 */
	QUANTITY_BASED,

	/**
	 * Object-based inventory (every item instance in its own object)
	 */
	OBJECT_BASED

}
