package org.asf.centuria.modules.events.levels;

import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.modules.eventbus.EventPath;
import org.asf.centuria.players.Player;

/**
 * 
 * Maintenance Start Event - called when server maintenance is started.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("level.join")
public class LevelJoinEvent extends EventObject {

	private int levelID;
	private String room;
	private Player player;

	public LevelJoinEvent(int levelID, String room, Player player) {
		this.levelID = levelID;
		this.room = room;
		this.player = player;
	}

	@Override
	public String eventPath() {
		return "level.join";
	}

	/**
	 * Retrieves the level ID of the world the player joined
	 * 
	 * @return Level ID
	 */
	public int getLevelID() {
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
