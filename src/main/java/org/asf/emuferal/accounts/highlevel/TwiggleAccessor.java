package org.asf.emuferal.accounts.highlevel;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.entities.inventoryitems.twiggles.TwiggleItem;
import org.asf.emuferal.entities.twiggles.TwiggleWorkParameters;

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

	public abstract TwiggleItem addNewTwiggle();

	public abstract void removeTwiggle();

	public abstract TwiggleItem setTwiggleWork(int workType, long workEndTime);

	public abstract TwiggleItem setTwiggleWork(int workType, long workEndTime,
			TwiggleWorkParameters twiggleWorkParameters);

	public abstract TwiggleItem[] getAllTwiggles();

	public abstract TwiggleItem getTwiggle(String twiggleInvId);

	public abstract TwiggleItem clearTwiggleWork(String twiggleInvId);

	public abstract void giveDefaultTwiggles();
}
