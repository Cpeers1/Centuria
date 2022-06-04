package org.asf.emuferal.social;

import org.asf.emuferal.social.impl.FileBasedSocialManager;

public abstract class SocialManager {

	protected static SocialManager implementation = new FileBasedSocialManager();

	/**
	 * Retrieves the DM manager
	 * 
	 * @return AbstractDMManager instance
	 */
	public static SocialManager getInstance() {
		return implementation;
	}

	/**
	 * Creates a new friend list on disk
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 */
	public abstract void openSocialList(String playerID);
	
	/**
	 * Deletes a social list on disk
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 */
	public abstract void deleteSocialList(String playerID);

	/**
	 * Checks if a player has a friend list that exists on the disk.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @return True if the friend list is saved on disk, false otherwise
	 */
	public abstract boolean socialListExists(String playerID);

	/**
	 * Retrieves the list entries of the players who this player has social contacts with.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @return Array of Friend List Entries.
	 */
	public abstract SocialEntry[] getSocialList(String playerID);
	
	/**
	 * Retrieves the list entries of the players who this player is following.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @return Array of Friend List Entries.
	 */
	public abstract SocialEntry[] getFollowingPlayers(String playerID);
	
	/**
	 * Retrieves the list entries of the players who are following the player.
	 * 
	 * @param playerID	ID for the main player (friend list owner)
	 * @return Array of Friend List Entries.
	 */
	public abstract SocialEntry[] getFollowerPlayers(String playerID);
	
	/**
	 * Gets if the player (by the player id to check) is in the players following list.
	 * 
	 * @param sourcePlayerID	ID for the main player (friend list owner)
	 * @param targetPlayerID	ID for the target player.
	 * @return A boolean value representing whether the main player has the target player in their following list.
	 */
	public abstract boolean getPlayerIsFollowing(String sourcePlayerID, String targetPlayerID);
	
	/**
	 * Gets if the player (by the player id to check) is in the players followers list.
	 * 
	 * @param sourcePlayerID	ID for the main player (friend list owner)
	 * @param targetPlayerID	ID for the target player.
	 * @return A boolean value representing whether the main player has the target player in their follower list.
	 */
	public abstract boolean getPlayerIsFollower(String sourcePlayerID, String targetPlayerID);
	
	/**
	 * Gets if the player (by the player id to check) is in the players blocked list.
	 * 
	 * @param sourcePlayerID	ID for the main player (friend list owner)
	 * @param targetPlayerID	ID for the target player.
	 * @return A boolean value representing whether the main player has the target player in their blocked list.
	 */
	public abstract boolean getPlayerIsBlocked(String sourcePlayerID, String targetPlayerID);
	
	/**
	 * Gets if the player (by the player id to check) is favorited.
	 * 
	 * @param sourcePlayerID	ID for the main player (friend list owner)
	 * @param targetPlayerID	ID for the target player.
	 * @return A boolean value representing whether the main player has the target player in their blocked list.
	 */
	public abstract boolean getPlayerIsFavorite(String sourcePlayerID, String targetPlayerID);

	/**
	 * Adds a new entry for a player who this player is following.
	 * 
	 * @param sourcePlayerID	ID for the main player (friend list owner)
	 * @param targetPlayerID The player list entry to add under the following section.
	 */
	public abstract void setFollowingPlayer(String sourcePlayerID, String targetPlayerID, boolean following);
	
	/**
	 * Adds a new entry for a player who is following this player.
	 * 
	 * @param sourcePlayerID	ID for the main player (friend list owner)
	 * @param targetPlayerID The player list entry to add under the followers section.
	 */
	public abstract void setFollowerPlayer(String sourcePlayerID, String targetPlayerID, boolean follower);

	/**
	 * Adds a new entry for a player who is blocked by this player.
	 * 
	 * @param sourcePlayerID	ID for the main player (friend list owner)
	 * @param targetPlayerID The player list entry to add under the blocked section.
	 */
	public abstract void setBlockedPlayer(String sourcePlayerID, String targetPlayerID, boolean blocked);
	
	/**
	 * Toggles the favorite status for the target player under their following list.
	 * 
	 * @param sourcePlayerID	ID for the main player (friend list owner)
	 * @param targetPlayerID The player list entry to add under the blocked section.
	 */
	public abstract void setFavoritePlayer(String sourcePlayerID, String targetPlayerID, boolean favorite);

}
