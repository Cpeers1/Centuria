package org.asf.emuferal.accounts.highlevel;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.entities.inventoryitems.twiggles.TwiggleItem;
import org.asf.emuferal.entities.twiggles.TwiggleWorkParameters;
import org.asf.emuferal.enums.twiggles.TwiggleState;

public abstract class TwiggleAccessor extends AbstractInventoryAccessor {

	/**
	 * Hard-Coded Twiggle Item Def Id. Since this will never change, this offers
	 * better performance then reading it off of the item list.
	 */
	public static final int TWIGGLE_DEF_ID = 8285;

	public static final int DEFAULT_TWIGGLE_AMOUNT = 3;

	public TwiggleAccessor(PlayerInventory inventory) {
		super(inventory);
	}

	/**
	 * Adds a new twiggle to the player's inventory.
	 * @return The new twiggle item that was added.
	 */
	public abstract TwiggleItem addNewTwiggle();

	/**
	 * Removes the first non-working twiggle from the player's inventory.
	 * @return True if successful, false if not.
	 */
	public abstract boolean removeTwiggle();
	
	/**
	 * Removes a specific twiggle from the player's inventory.
	 * @param twiggleInvId The inventory id of the twiggle.
	 * @return True if successful, false if not.
	 */
	public abstract boolean removeTwiggle(String twiggleInvId);

	/**
	 * Gives a non-working twiggle a new work item.
	 * @param workType The type of work being done. 
	 * @param workEndTime The end time of the work being done.
	 * @return The updated twiggle item, or null if there was no twiggle to give work to.
	 */
	public abstract TwiggleItem setTwiggleWork(TwiggleState workType, long workEndTime);

	/**
	 * Gives a non-working twiggle a new work item.
	 * @param workType The type of work being done. 
	 * @param workEndTime The end time of the work being done.
	 * @param twiggleWorkParameters additional work parameters for the twiggle.
	 * @return The updated twiggle item, or null if there was no twiggle to give work to.
	 */
	public abstract TwiggleItem setTwiggleWork(TwiggleState workType, long workEndTime,
			TwiggleWorkParameters twiggleWorkParameters);

	/**
	 * Retrieves an array of all the twiggles in the player's inventory.
	 * @return An array of all the twiggles in the player's inventory.
	 */
	public abstract TwiggleItem[] getAllTwiggles();

	/**
	 * Gets a specific twiggle item with its inventory id.
	 * @param twiggleInvId The inventory id of the twiggle.
	 * @return The twiggle with the inventory id, or null if there was no match.
	 */
	public abstract TwiggleItem getTwiggle(String twiggleInvId);

	/**
	 * Reset's a twiggle with a specific inventory id, clearing any work it is doing.
	 * @param twiggleInvId The inventory ID of the twiggle doing the work.
	 * @return The updated twiggle item, or null if there was no match to the given ID.
	 */
	public abstract TwiggleItem clearTwiggleWork(String twiggleInvId);

	/**
	 * Gives default twiggle items.
	 */
	public abstract void giveDefaultTwiggles();
}
