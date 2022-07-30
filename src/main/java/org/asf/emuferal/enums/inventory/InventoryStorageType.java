package org.asf.emuferal.enums.inventory;

public enum InventoryStorageType {

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
