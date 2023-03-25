package org.asf.centuria.accounts.highlevel;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.entities.sanctuaries.RoomInfoObject;
import org.asf.centuria.entities.sanctuaries.SanctuaryObjectData;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public abstract class SanctuaryAccessor extends AbstractInventoryAccessor {
	public SanctuaryAccessor(PlayerInventory inventory) {
		super(inventory);
	}

	/**
	 * Finds all unlocked island types
	 * 
	 * @return Array of island type defIDs
	 */
	public abstract int[] getUnlockedIslandTypes();

	/**
	 * Retrieves the count of items of a specific island type
	 * 
	 * @param defID Island type ID
	 * @return Item count of the given island type
	 */
	public abstract int getIslandTypeItemCount(int defID);

	/**
	 * Creates a new save for a specific island type and saves it to the inventory
	 * 
	 * @param defID Island type ID
	 * @return Item ID string
	 */
	public abstract String addIslandToInventory(int defID);

	/**
	 * Finds a island info object
	 * 
	 * @param id Island info object ID
	 * @return JSON object or null
	 */
	public abstract JsonObject getIslandTypeObject(String id);

	/**
	 * Checks if a island type is unlocked or not
	 * 
	 * @param defID Island type ID
	 * @return True if unlocked, false otherwise
	 */
	public abstract boolean isIslandTypeUnlocked(int defID);

	/**
	 * Finds all unlocked house types
	 * 
	 * @return Array of house type defIDs
	 */
	public abstract int[] getUnlockedHouseTypes();

	/**
	 * Retrieves the count of items of a specific house type
	 * 
	 * @param defID House type ID
	 * @return Item count of the given house type
	 */
	public abstract int getHouseTypeItemCount(int defID);

	/**
	 * Creates a new save for a specific house type and saves it to the inventory
	 * 
	 * @param defID House type ID
	 * @return Item ID string
	 */
	public abstract String addHouseToInventory(int defID);

	/**
	 * Finds a house info object
	 * 
	 * @param id House info object ID
	 * @return JSON object or null
	 */
	public abstract JsonObject getHouseTypeObject(String id);

	/**
	 * Checks if a house type is unlocked or not
	 * 
	 * @param defID House type ID
	 * @return True if unlocked, false otherwise
	 */
	public abstract boolean isHouseTypeUnlocked(int defID);

	/**
	 * Finds all unlocked sanctuary classes
	 * 
	 * @return Array of sanctuary class defIDs
	 */
	public abstract int[] getUnlockedSanctuaries();

	/**
	 * Unlocks sanctuaries
	 * 
	 * @param defID Sanctuary class ID
	 */
	public abstract void unlockSanctuary(int defID);

	/**
	 * Finds a sanctuary class object
	 * 
	 * @param id Sanctuary class object ID
	 * @return JSON object or null
	 */
	public abstract JsonObject getSanctuaryClassObject(String id);

	/**
	 * Checks if a sanctuary class is unlocked or not
	 * 
	 * @param defID Sanctuary class ID
	 * @return True if unlocked, false otherwise
	 */
	public abstract boolean isSanctuaryUnlocked(int defID);

	/**
	 * Retrieves the sanctuary look count
	 * 
	 * @return The amount of sanctuary looks a player has
	 */
	public abstract int getSanctuaryLookCount();

	/**
	 * Finds a sanctuary look
	 * 
	 * @param lookID Sanctuary look ID
	 * @return Sanctuary JSON object or null
	 */
	public abstract JsonObject getSanctuaryLook(String lookID);

	/**
	 * Retrieves the first sanctuary look found in the inventory
	 * 
	 * @return Sanctuary JSON object or null
	 */
	public abstract JsonObject getFirstSanctuaryLook();

	/**
	 * Retrieves an array of sanctuary look IDs
	 * 
	 * @return Array of sanctuary look IDs
	 */
	public abstract String[] getSanctuaryLookIDs();

	/**
	 * Adds an extra sanctuary look slot to the player inventory
	 */
	public abstract void addExtraSanctuarySlot();

	/**
	 * Adds a object to the player's sanctuary.
	 */
	public abstract boolean addSanctuaryObject(String objectUUID, SanctuaryObjectData positionalInfo,
			String activeSancLookId);

	/**
	 * Removes a object from the player's sanctuary.
	 */
	public abstract void removeSanctuaryObject(String objectUUID, String activeSancLookId);

	/**
	 * Updates room info for the player's sanctuary.
	 */
	public abstract JsonObject updateSanctuaryRoomData(String activeSancLookId, RoomInfoObject[] roomInfos);

	/**
	 * Saves the currently active sanctuary look to a new slot with the ID provided.
	 */
	public abstract void saveSanctuaryLookToSlot(String activeSancLookId, String slotId, String saveName);

	/**
	 * Upgrades the class of sanctuary to the specific stage.
	 * 
	 * @param sancClassInvId The inv id of the sanctuary class to upgrade.
	 * @param stage          The stage of the sanctuary to upgrade to.
	 */
	public abstract boolean upgradeSanctuaryToStage(String sancClassInvId, int stage);

	/**
	 * Enlargens the room of a specific sanctuary type.
	 * 
	 * @param sancClassInvId The inv id of the sanctuary class to upgrade.
	 * @param roomIndex      The room index to set to 1
	 * @param isEnlargen     Choose to set index to 1 or 0
	 */
	public abstract boolean modifySancturaryRoomUpgradeState(String sancClassInvId, int roomIndex, Boolean isEnlargen);

	/**
	 * Gets the current sanctuary stage.
	 * 
	 * @param sancClassInvId The inv id of the sanctuary class.
	 * @return The sanctuary stage.
	 */
	public abstract int getCurrentSanctuaryStage(String sancClassInvId);

	/**
	 * Gets the current sanctuary expansion array
	 * 
	 * @param sancClassInvId The inv id of the sanctuary class.
	 * @return The sanctuary expansion Array.
	 */
	public abstract JsonArray getExpandedRooms(String sancClassInvId);

}