package org.asf.centuria.rooms;

import java.util.stream.Stream;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.gameserver.GameServer;

/**
 * 
 * Game Sync Room
 * 
 * @author Sky Swimmer
 * 
 */
public class GameRoom {

	private String id;
	private String instID;
	private int levelID;
	private GameServer server;

	/**
	 * Controls if this room should be allowed to be selected by the game room
	 * manager's findBestRoom call
	 */
	public boolean allowSelection = false;

	public GameRoom(String id, String instID, int levelID, GameServer server) {
		this.id = id;
		this.instID = instID;
		this.levelID = levelID;
		this.server = server;
	}

	/**
	 * Retrieves the room ID
	 * 
	 * @return Room ID string
	 */
	public String getID() {
		return id;
	}

	/**
	 * Retrieves the room instance ID
	 * 
	 * @return Room instance ID string
	 */
	public String getInstanceID() {
		return instID;
	}

	/**
	 * Retrieves the room level ID
	 * 
	 * @return Room level ID
	 */
	public int getLevelID() {
		return levelID;
	}

	/**
	 * Retrieves all players in the room
	 * 
	 * @return Array of Player instances
	 */
	public Player[] getPlayers() {
		return Stream.of(server.getPlayers()).filter(t -> t.room != null && t.room.equalsIgnoreCase(id))
				.toArray(t -> new Player[t]);
	}

}
