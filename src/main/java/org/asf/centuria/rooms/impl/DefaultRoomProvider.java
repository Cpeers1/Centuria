package org.asf.centuria.rooms.impl;

import java.util.HashMap;

import org.asf.centuria.Centuria;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.rooms.GameRoom;
import org.asf.centuria.rooms.GameRoomManager;
import org.asf.centuria.rooms.IRoomProvider;

public class DefaultRoomProvider implements IRoomProvider {

	@Override
	public GameRoom findBestRoom(int levelID, Player requester, GameRoomManager manager,
			HashMap<GameRoom, Integer> rooms) {// Load settings
		int preferredMin = 15;
		int preferredMax = 50;
		try {
			preferredMin = Integer.parseInt(Centuria.serverProperties.getOrDefault("room-preferred-min-players", "15"));
		} catch (Exception e) {
		}
		try {
			preferredMax = Integer.parseInt(Centuria.serverProperties.getOrDefault("room-preferred-max-players", "50"));
		} catch (Exception e) {
		}

		// Find rooms that match, first those below minimum
		for (GameRoom room : rooms.keySet().stream().sorted((t1, t2) -> -Integer.compare(rooms.get(t1), rooms.get(t2)))
				.toArray(t -> new GameRoom[t])) {
			int playerCount = rooms.get(room);
			if (playerCount < preferredMin)
				return room; // Found one
		}

		// Find rooms that match, first those below maximum
		for (GameRoom room : rooms.keySet().stream().sorted((t1, t2) -> -Integer.compare(rooms.get(t1), rooms.get(t2)))
				.toArray(t -> new GameRoom[t])) {
			int playerCount = rooms.get(room);
			if (playerCount < preferredMax)
				return room; // Found one
		}

		// Couldnt find a room
		return null;
	}

}
