package org.asf.centuria.rooms;

import java.util.HashMap;

import org.asf.centuria.entities.players.Player;

/**
 * 
 * Game room selection provider
 * 
 * @author Sky Swimmer
 * 
 */
public interface IRoomProvider {

	/**
	 * Finds the best room for a player
	 * 
	 * @param levelID   Level ID
	 * @param requester Requesting player
	 * @param manager   Room manager
	 * @param rooms     Map of rooms with player count as value
	 * @return GameRoom instance or null to move to the next provider
	 */
	public GameRoom findBestRoom(int levelID, Player requester, GameRoomManager manager,
			HashMap<GameRoom, Integer> rooms);

}
