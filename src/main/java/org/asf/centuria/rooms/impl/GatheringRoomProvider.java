package org.asf.centuria.rooms.impl;

import java.util.HashMap;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.rooms.GameRoom;
import org.asf.centuria.rooms.GameRoomManager;
import org.asf.centuria.rooms.IRoomProvider;

public class GatheringRoomProvider implements IRoomProvider {

	public static boolean enabled;

	@Override
	public GameRoom findBestRoom(int levelID, Player requester, GameRoomManager manager,
			HashMap<GameRoom, Integer> rooms) {
		if (enabled) {
			// Send players to a 'gathering' room for eg. events
			return manager.getOrCreateRoom(levelID, "GATHERING");
		}
		return null;
	}

}
