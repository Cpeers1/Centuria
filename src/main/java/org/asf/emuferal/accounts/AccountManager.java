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
	 * Checks if there is a request saved to change the password (should override
	 * authenticate and save the password entered during login)
	 * 
	 * @param userID Account ID
	 * @return True if there is a password update request, false otherwise
	 */
	public abstract boolean isPasswordUpdateRequested(String userID);

	/**
	 * Schedules a password update for next login
	 * 
	 * @param userID Account ID
	 */
	public abstract void makePasswordUpdateRequested(String userID);

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
