package org.asf.centuria.minigames;

import java.util.ArrayList;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.minigames.games.GameTwiggleBuilders;
import org.asf.centuria.minigames.games.GameWhatTheHex;

/**
 * 
 * Minigame manager
 * 
 * @author Sky Swimmer
 *
 */
public class MinigameManager {

	private static ArrayList<AbstractMinigame> minigames = new ArrayList<AbstractMinigame>();

	static {
		// Default minigames
		registerMinigame(new GameWhatTheHex());
		registerMinigame(new GameTwiggleBuilders());
	}

	/**
	 * Registers a minigame
	 * 
	 * @param game Minigame to register
	 */
	public static void registerMinigame(AbstractMinigame game) {
		minigames.add(game);
	}

	/**
	 * Finds the minigame the player is in
	 * 
	 * @param player Player to find the minigame for
	 * @return AbstractMinigame instance or null
	 */
	public static AbstractMinigame getGameFor(Player player) {
		return getGameFor(player.levelID);
	}

	/**
	 * Finds the minigame for a minigame level ID
	 * 
	 * @param levelID Minigame level ID
	 * @return AbstractMinigame instance or null
	 */
	public static AbstractMinigame getGameFor(int levelID) {
		for (AbstractMinigame game : minigames) {
			if (game.canHandle(levelID))
				return game;
		}
		return null;
	}

}
