package org.asf.centuria.rooms.impl;

import java.util.HashMap;
import java.util.Optional;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.rooms.GameRoom;
import org.asf.centuria.rooms.GameRoomManager;
import org.asf.centuria.rooms.IRoomProvider;

public class ModerationRoomProvider implements IRoomProvider {

	@Override
	public GameRoom findBestRoom(int levelID, Player requester, GameRoomManager manager,
			HashMap<GameRoom, Integer> rooms) {
		if (requester != null) {
			// Load permission level
			String permLevel = "member";
			if (requester.account.getSaveSharedInventory().containsItem("permissions")) {
				permLevel = requester.account.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
						.get("permissionLevel").getAsString();
			}

			// Check moderator
			if (GameServer.hasPerm(permLevel, "moderator") && !GameServer.hasPerm(permLevel, "developer")) {
				// Select room with most players
				Optional<GameRoom> room = rooms.keySet().stream()
						.sorted((t1, t2) -> -Integer.compare(rooms.get(t1), rooms.get(t2))).findFirst();
				if (room.isPresent())
					return room.get(); // Found one
			}
		}
		return null;
	}

}
