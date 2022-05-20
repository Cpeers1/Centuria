package org.asf.emuferal.accounts;

import org.asf.emuferal.accounts.impl.FileBasedAccountManager;

public abstract class AccountManager {

	// Account manager implementation
	private static AccountManager instance = new FileBasedAccountManager();

	/**
	 * Retrieves the active account manager
	 * 
	 * @return AccountManager instance
	 */
	public static AccountManager getInstance() {
		return instance;
	}

	/**
	 * Authenticates a player login and returns a account ID
	 * 
	 * @param username Player login username
	 * @param password Player password
	 * @return Account ID or null if invalid
	 */
	public abstract String authenticate(String username, char[] password);

	/**
	 * Registers accounts
	 * 
	 * @param username Account login name
	 * @return Account ID or null if invalid
	 */
	public abstract String register(String username);

	/**
	 * Checks if the user has been fully registered
	 * 
	 * @param userID Account ID
	 * @return True if the password is saved, false otherwise
	 */
	public abstract boolean hasPassword(String userID);

	/**
	 * Completes the registration process
	 * 
	 * @param userID      Account ID
	 * @param displayName New player display name
	 * @param password    Player password
	 * @return True if successful, false otherwise
	 */
	public boolean finishRegistration(String userID, String displayName, char[] password) {
		if (!getAccount(userID).updateDisplayName(displayName))
			return false;
		return updatePassword(userID, password);
	}

	/**
	 * Updates the user password
	 * 
	 * @param userID   Account ID
	 * @param password Player password
	 * @return True if successful, false otherwise
	 */
	public abstract boolean updatePassword(String userID, char[] password);

	/**
	 * Retrieves EmuFeralAccount instance by ID
	 * 
	 * @param userID Account ID
	 * @return EmuFeralAccount instance
	 */
	public abstract EmuFeralAccount getAccount(String userID);

}
