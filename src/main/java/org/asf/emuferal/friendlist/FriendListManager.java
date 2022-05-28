package org.asf.emuferal.friendlist;

import org.asf.emuferal.dms.impl.FileBasedDMManager;
import org.asf.emuferal.friendlist.impl.FileBasedFriendListManager;

public abstract class FriendListManager {

	private static FriendListManager implementation = new FileBasedFriendListManager();

	/**
	 * Retrieves the DM manager
	 * 
	 * @return AbstractDMManager instance
	 */
	public static FriendListManager getInstance() {
		return implementation;
	}

	/**
	 * Creates a new friend list on disk
	 * 
	 * @param dmID Id of the player for the friend list.
	 */
	public abstract void openFriendList(String playerID);

	/**
	 * Checks if a player has a friend list that exists on the disk.
	 * 
	 * @param playerID for the main player.
	 * @return True if the friend list is saved on disk, false otherwise
	 */
	public abstract boolean friendListExists(String playerID);

	/**
	 * Retrieves the list entries of the players who this player is following.
	 * 
	 * @param playerID for the main player.
	 * @return Array of Friend List Entries.
	 */
	public abstract FriendListEntry[] getFollowingList(String playerID);
	
	/**
	 * Retrieves the list entries of the players who are following this player.
	 * 
	 * @param playerID for the main player.
	 * @return Array of Friend List Entries.
	 */
	public abstract FriendListEntry[] getFollowerList(String playerID);


	/**
	 * Retrieves the list entries of the players who this player has blocked.
	 * 
	 * @param playerID for the main player.
	 * @return Array of PrivateChatMessage instances
	 */
	public abstract FriendListEntry[] getBlockedList(String playerID);

	/**
	 * Adds a new entry for a player who this player is following.
	 * 
	 * @param playerID    The id of the player 
	 * @param playerToAdd The player list entry to add under the following section.
	 */
	public abstract void addFollowingPlayer(String playerID, FriendListEntry playerToAdd);
	
	/**
	 * Adds a new entry for a player who is following this player.
	 * 
	 * @param playerID    The id of the player 
	 * @param playerToAdd The player list entry to add under the followers section.
	 */
	public abstract void addFollowerPlayer(String playerID, FriendListEntry playerToAdd);

	/**
	 * Adds a new entry for a player who is blocked by this player.
	 * 
	 * @param playerID    The id of the player 
	 * @param playerToAdd The player list entry to add under the blocked section.
	 */
	public abstract void addBlockedPlayer(String playerID, FriendListEntry playerToAdd);
}
