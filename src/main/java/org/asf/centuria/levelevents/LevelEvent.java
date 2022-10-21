package org.asf.centuria.levelevents;

import org.asf.centuria.entities.players.Player;

/**
 * 
 * Level Event Object
 * 
 * @author Sky Swimmer
 *
 */
public class LevelEvent {

	private Player player;
	private String type;
	private String[] tags;

	public LevelEvent(String type, String[] tags, Player player) {
		this.type = type;
		this.tags = tags;
		this.player = player;
	}

	/**
	 * Retrieves the event type name
	 * 
	 * @return Type string
	 */
	public String getType() {
		return type;
	}

	/**
	 * Retrieves the event tag array
	 * 
	 * @return Array of tag strings
	 */
	public String[] getTags() {
		return tags;
	}

	/**
	 * Retrieves the player calling the event
	 * 
	 * @return Player instance
	 */
	public Player getPlayer() {
		return player;
	}

}
