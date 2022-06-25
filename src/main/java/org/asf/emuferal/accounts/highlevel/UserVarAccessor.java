package org.asf.emuferal.accounts.highlevel;
import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.entities.uservars.SetUserVarResult;
import org.asf.emuferal.entities.uservars.UserVarValue;

import java.util.HashMap;

public abstract class UserVarAccessor extends AbstractInventoryAccessor {
	
	public UserVarAccessor(PlayerInventory inventory) {
		super(inventory);
	}
	
	/**
	 * Sets a player variable using the passed defID for the variable, 
	 * and the values for that variable.
	 * This method overrides all set values using the index number from the array.
	 * @param defID The ID for the variable to set values for.
	 * @param values The values to set for the variable.
	 * @return The result for setting the variable.
	 */
	public abstract SetUserVarResult setPlayerVarValue(int defID, int[] values);
	
	/**
	 * Sets a player variable using the passed defID for the variable, at the specific index.
	 * @param defID The ID for the variable to set values for.
	 * @param index The index to update in the variable.
	 * @param value The value to put at that index.
	 * @return The result for setting the variable.
	 */
	public abstract SetUserVarResult setPlayerVarValue(int defID, int index, int value);

	/**
	 * Sets a player variable using the passed DefID for the variable,
	 * and also a map of indexes and values to set for the index in the variable.
	 * @param defID The ID for the variable.
	 * @param indexToValueUpdateMap A map of indexes and values to set for the index in the variable.
	 * @return The result for setting the variable.
	 */
	public abstract SetUserVarResult setPlayerVarValue(int defID, HashMap<Integer, Integer> indexToValueUpdateMap);
	
	/**
	 * Gets all the values for the player variable.
	 * @param defID The ID for the variable to get values from.
	 * @return A map of index --> values for all the values in the player variable.
	 */
	public abstract UserVarValue[] getPlayerVarValue(int defID);
	
	/**
	 * Gets the value for the player variable for the specified index.
	 * @param defID The ID for the variable to get values from.
	 * @param index The player variable index to get the value at.
	 * @return The value at that index in the player variable.
	 */
	public abstract UserVarValue getPlayerVarValue(int defID, int index);
	
	/**
	 * Gets the values for the player variable for all the specified indexes.
	 * @param defID The ID for the variable to get values from.
	 * @param indexes The indexes to retrieve values from.
	 * @return A map of index --> values of the retrieved values.
	 */
	public abstract UserVarValue[] getPlayerVarValue(int defID, int[] indexes);
	
	/**
	 * Deletes a player variable from the player's inventory, along with all its values.
	 * You will need to send an IL packet to the client afterwards to update their inventory.
	 * @param defID The player variable ID to delete.
	 * @return Whether the deletion was a success or not.
	 */
	public abstract boolean deletePlayerVar(int defID);
	
	/**
	 * Deletes a player variable value at the index from the player's inventory.
	 * Also reindexes the other values to make up from the missing value.
	 * @param defID The player variable ID to delete a value from.
	 * @param index The index to delete the value at.
	 * @return The result of the deletion.
	 */
	public abstract boolean deletePlayerVarValueAtIndex(int defID, int index);
	
	
}

