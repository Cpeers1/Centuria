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
	 * @param playerID	ID for the main player (friend list owner)
	 */
	public abstract void openFriendList(String playerID);

	/**
	 * Checks if a player has a friend list that exists on the disk.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @return True if the friend list is saved on disk, false otherwise
	 */
	public abstract boolean friendListExists(String playerID);

	/**
	 * Retrieves the list entries of the players who this player is following.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @return Array of Friend List Entries.
	 */
	public abstract FriendListEntry[] getFollowingList(String playerID);
	
	/**
	 * Retrieves the list entries of the players who are following this player.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @return Array of Friend List Entries.
	 */
	public abstract FriendListEntry[] getFollowerList(String playerID);


	/**
	 * Retrieves the list entries of the players who this player has blocked.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @return Array of PrivateChatMessage instances
	 */
	public abstract FriendListEntry[] getBlockedList(String playerID);
	
	/**
	 * Gets if the player (by the player id to check) is in the players following list.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @param playerIDToCheck	ID for the target player.
	 * @return A boolean value representing whether the main player has the target player in their following list.
	 */
	public abstract Boolean getPlayerIsFollowing(String playerID, String playerIDToCheck);
	
	/**
	 * Gets if the player (by the player id to check) is in the players followers list.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @param playerIDToCheck	ID for the target player.
	 * @return A boolean value representing whether the main player has the target player in their follower list.
	 */
	public abstract Boolean getPlayerIsFollower(String playerID, String playerIDToCheck);
	
	/**
	 * Gets if the player (by the player id to check) is in the players blocked list.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @param playerIDToCheck	ID for the target player.
	 * @return A boolean value representing whether the main player has the target player in their blocked list.
	 */
	public abstract Boolean getPlayerIsBlocked(String playerID, String playerIDToCheck);

	/**
	 * Adds a new entry for a player who this player is following.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @param playerToAdd The player list entry to add under the following section.
	 */
	public abstract void addFollowingPlayer(String playerID, FriendListEntry playerToAdd);
	
	/**
	 * Adds a new entry for a player who is following this player.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @param playerToAdd The player list entry to add under the followers section.
	 */
	public abstract void addFollowerPlayer(String playerID, FriendListEntry playerToAdd);

	/**
	 * Adds a new entry for a player who is blocked by this player.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @param playerToAdd The player list entry to add under the blocked section.
	 */
	public abstract void addBlockedPlayer(String playerID, FriendListEntry playerToAdd);
	
	/**
	 * Adds a new entry for a player who this player is following.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @param playerToAdd The player list entry to add under the following section.
	 */
	public abstract void removeFollowingPlayer(String playerID, String playerIDToRemove);
	
	/**
	 * Adds a new entry for a player who is following this player.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @param playerToAdd The player list entry to add under the followers section.
	 */
	public abstract void removeFollowerPlayer(String playerID, String playerIDToRemove);

	/**
	 * Adds a new entry for a player who is blocked by this player.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @param playerToAdd The player list entry to add under the blocked section.
	 */
	public abstract void removeBlockedPlayer(String playerID, String playerIDToRemove);
	
	/**
	 * Toggles the favorite status for the target player under their following list.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @param targetPlayerID The player list entry to add under the blocked section.
	 */
	public abstract void toggleFollowingPlayerAsFavorite(String playerID, String targetPlayerID);
	
	/**
	 * Toggles the favorite status for the target player under their follower list.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @param targetPlayerID The player list entry to add under the blocked section.
	 */
	public abstract void toggleFollowerPlayerAsFavorite(String playerID, String targetPlayerID);
	
}
