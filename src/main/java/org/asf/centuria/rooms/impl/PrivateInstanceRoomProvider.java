package org.asf.centuria.rooms.impl;

import java.util.HashMap;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.rooms.GameRoom;
import org.asf.centuria.rooms.GameRoomManager;
import org.asf.centuria.rooms.IRoomProvider;
import org.asf.centuria.rooms.privateinstances.PrivateInstance;
import org.asf.centuria.rooms.privateinstances.containervars.PrivateInstanceTeleportVars;

public class PrivateInstanceRoomProvider implements IRoomProvider {

	private GameServer server;

	public PrivateInstanceRoomProvider(GameServer server) {
		this.server = server;
	}

	@Override
	public GameRoom findBestRoom(int levelID, Player requester, GameRoomManager manager,
			HashMap<GameRoom, Integer> rooms) {
		// Check
		if (requester != null) {
			// Find private instance
			PrivateInstance instance = server.getPrivateInstanceManager()
					.getSelectedInstanceOf(requester.account.getAccountID());

			// Check vars
			PrivateInstanceTeleportVars vars = requester.getObject(PrivateInstanceTeleportVars.class);
			if (vars != null) {
				// Check disable
				if (vars.disableInstanceTeleport)
					return null;

				// Check if forcefully connected and if not, check gathering
				if (!vars.forcefullyConnectedToInstance && GatheringRoomProvider.enabled)
					return null; // Let the gathering system do this

				// Check if another instance should be used
				if (vars.selectedInstance != null
						&& (vars.selectedInstance.isParticipant(requester.account.getAccountID())
								|| requester.hasModPerms))
					instance = vars.selectedInstance;
				else if (vars.selectedInstance != null)
					vars.selectedInstance = null;
			}

			// Check
			if (instance != null)
				return instance.getRoom(levelID);
		}

		// Couldnt find a room
		return null;
	}

}
