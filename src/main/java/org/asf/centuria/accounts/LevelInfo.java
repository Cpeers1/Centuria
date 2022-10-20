package org.asf.centuria.accounts;

import org.asf.centuria.entities.players.Player;

public abstract class LevelInfo {

	/**
	 * Checks if the player level is available
	 * 
	 * @return True if the level is available, false otherwise
	 */
	public abstract boolean isLevelAvailable();

	/**
	 * Retrieves the player level
	 * 
	 * @return Player level or -1
	 */
	public abstract int getLevel();

	/**
	 * Retrieves the player total XP
	 * 
	 * @return Player total XP or -1
	 */
	public abstract int getTotalXP();

	/**
	 * Retrieves the player XP for this level
	 * 
	 * @return Player XP or -1
	 */
	public abstract int getCurrentXP();

	/**
	 * Retrieves the total XP for this level to reach the next level
	 * 
	 * @return Level-up XP or -1
	 */
	public abstract int getLevelupXPCount();

	/**
	 * Adds XP to the player
	 * 
	 * @param xp XP to give
	 */
	public abstract void addXP(int xp);

	/**
	 * Called to handle world join
	 * 
	 * @param player Player that joined the world
	 */
	public abstract void onWorldJoin(Player player);

}
