package org.asf.centuria.accounts;

/**
 * 
 * Save Manager for servers with Multi-save support
 * 
 * @author Sky Swimmer
 *
 */
public abstract class SaveManager {

	/**
	 * Retrieves a save-specific inventory for a specific save
	 * 
	 * @since Beta 1.6
	 * @param save Save name
	 * @return PlayerInventory instance or null if invalid
	 */
	public abstract PlayerInventory getSaveSpecificInventoryOf(String save);

	/**
	 * Retrieves the settings of a specific save
	 * 
	 * @since Beta 1.6.5
	 * @param save Save name
	 * @return SaveSettings instance or null if invalid
	 */
	public SaveSettings getSaveSettingsOf(String save) {
		PlayerInventory inv = getSaveSpecificInventoryOf(save);
		if (inv == null)
			return null;
		return inv.getSaveSettings();
	}

	/**
	 * Retrieves the current active save name
	 * 
	 * @return Active save name
	 */
	public abstract String getCurrentActiveSave();

	/**
	 * Checks if a save exists
	 * 
	 * @param save Save name
	 * @return True if the save exists, false otherwise
	 */
	public abstract boolean saveExists(String save);

	/**
	 * Creates a save
	 * 
	 * @param save Save name
	 * @return True if successful, false if invalid
	 */
	public abstract boolean createSave(String save);

	/**
	 * Deletes a save
	 * 
	 * @param save Save name
	 * @return True if successful, false if invalid
	 */
	public abstract boolean deleteSave(String save);

	/**
	 * Switches the active save
	 * 
	 * @param save Save name
	 * @return True if successful, false otherwise
	 */
	public abstract boolean switchSave(String save);

	/**
	 * Retrieves an array of save names
	 * 
	 * @return Array of save name strings
	 */
	public abstract String[] getSaves();

}
