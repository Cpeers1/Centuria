package org.asf.centuria.modules.events.levels;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.modules.eventbus.EventObject;

/**
 * 
 * Maintenance Start Event - called when server maintenance is started.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class LevelJoinEvent extends EventObject {

	private String levelID;
	private String room;
	private Player player;

	public LevelJoinEvent(String levelID, String room, Player player) {
		this.levelID = levelID;
		this.room = room;
		this.player = player;
	}

	/**
	 * Retrieves the level ID of the world the player joined
	 * 
	 * @return Level ID
	 */
	public String getLevelID() {
		return levelID;
	}

	/**
	 * Retrieves the room ID
	 * 
	 * @return Chat and sync room ID
	 */
	public String getRoomId() {
		return room;
	}

	/**
	 * Retrieves the player
	 * 
	 * @return Player instance
	 */
	public Player getPlayer() {
		return player;
	}

}
