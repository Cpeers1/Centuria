package org.asf.centuria.accounts;

import org.asf.centuria.accounts.impl.FileBasedAccountManager;

public abstract class AccountManager {

	// Account manager implementation
	protected static AccountManager instance = new FileBasedAccountManager();

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
	 * Retrieves user IDs by display name
	 * 
	 * @param displayName Player display name
	 * @return Player ID or null
	 */
	public abstract String getUserByDisplayName(String displayName);

	/**
	 * Retrieves user IDs by login name
	 * 
	 * @param loginName Player login name
	 * @return Player ID or null
	 */
	public abstract String getUserByLoginName(String loginName);

	/**
	 * Checks if a display name is in use
	 * 
	 * @param displayName Player display name
	 * @return True if in use, false otherwise
	 */
	public abstract boolean isDisplayNameInUse(String displayName);

	/**
	 * Releases a display name lock
	 * 
	 * @param displayName Player display name
	 * @return True if successful, false otherwise
	 */
	public abstract boolean releaseDisplayName(String displayName);

	/**
	 * Locks a display name
	 * 
	 * @param displayName Player display name
	 * @param userID      Owner player ID
	 * @return True if successful, false otherwise
	 */
	public abstract boolean lockDisplayName(String displayName, String userID);

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
	 * Retrieves CenturiaAccount instance by ID
	 * 
	 * @param userID Account ID
	 * @return CenturiaAccount instance
	 */
	public abstract CenturiaAccount getAccount(String userID);

}
