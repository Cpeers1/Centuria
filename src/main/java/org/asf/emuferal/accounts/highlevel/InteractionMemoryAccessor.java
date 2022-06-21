package org.asf.emuferal.accounts.highlevel;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;

public abstract class InteractionMemoryAccessor extends AbstractInventoryAccessor {

	public InteractionMemoryAccessor(PlayerInventory inventory) {
		super(inventory);
	}

	/**
	 * Prepares level interaction memory
	 * 
	 * @param level Level ID
	 */
	public abstract void prepareLevel(int level);

	/**
	 * Prepares a harvest entry
	 * 
	 * @param levelID Level ID
	 * @param itemID  Harvestable interactable UUID
	 */
	public abstract void prepareHarvestItem(int levelID, String itemID);

	/**
	 * Prepares a treasure entry
	 * 
	 * @param levelID Level ID
	 * @param itemID  Treasure interactable UUID
	 */
	public abstract void prepareTreasureItem(int levelID, String itemID);

	/**
	 * Prepares a daily task entry
	 * 
	 * @param levelID Level ID
	 * @param itemID  Task UUID
	 */
	public abstract void prepareDailyTaskEntry(int levelID, String itemID);

	/**
	 * Retrieves the last harvest timestamp
	 * 
	 * @param levelID Level ID
	 * @param itemID  Harvestable interactable UUID
	 * @return Last harvest timestamp or 0 or -1 if never harvested before
	 */
	public abstract long getLastHarvestTime(int levelID, String itemID);

	/**
	 * Retrieves the last harvest count
	 * 
	 * @param levelID Level ID
	 * @param itemID  Harvestable interactable UUID
	 * @return Last harvest count
	 */
	public abstract int getLastHarvestCount(int levelID, String itemID);
	
	/**
	 * Resets the harvest count for the object.
	 * @param levelID Level ID
	 * @param itemID  Harvestable interactable UUID
	 */
	public abstract void resetHarvestCount(int levelID, String itemID);

	/**
	 * Retrieves the last treasure unlock timestamp
	 * 
	 * @param levelID Level ID
	 * @param itemID  Treasure interactable UUID
	 * @return Last unlock timestamp or 0 or -1 if never unlocked before
	 */
	public abstract long getLastTreasureUnlockTime(int levelID, String itemID);

	/**
	 * Retrieves the if a treasure chest has been unlocked
	 * 
	 * @param levelID Level ID
	 * @param itemID  Treasure interactable UUID
	 * @return True if the chest was unlocked, false otherwise
	 */
	public abstract boolean hasTreasureBeenUnlocked(int levelID, String itemID);

	/**
	 * Retrieves the last time on which a daily task was completed
	 * 
	 * @param levelID Level ID
	 * @param itemID  Task UUID
	 * @return Last task completion timestamp or 0 or -1 if never done before
	 */
	public abstract long getLastDailyTaskTime(int levelID, String itemID);

	/**
	 * Called on harvest completion
	 * 
	 * @param levelID Level ID
	 * @param itemID  Harvestable interactable UUID
	 */
	public abstract void harvested(int levelID, String itemID);

	/**
	 * Called on treasure unlock
	 * 
	 * @param levelID Level ID
	 * @param itemID  Treasure interactable UUID
	 */
	public abstract void unlocked(int levelID, String itemID);

	/**
	 * Called on daily task completion
	 * 
	 * @param levelID Level ID
	 * @param itemID  Task UUID
	 */
	public abstract void completedTask(int levelID, String itemID);

	/**
	 * Saves interaction memory and writes it to a client
	 * 
	 * @param client Client to update
	 */
	public abstract void saveTo(SmartfoxClient client);

}
