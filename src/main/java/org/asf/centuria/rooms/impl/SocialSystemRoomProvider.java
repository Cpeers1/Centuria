package org.asf.centuria.rooms.impl;

import java.util.HashMap;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.rooms.GameRoom;
import org.asf.centuria.rooms.GameRoomManager;
import org.asf.centuria.rooms.IRoomProvider;
import org.asf.centuria.social.SocialEntry;
import org.asf.centuria.social.SocialManager;

public class SocialSystemRoomProvider implements IRoomProvider {

	@Override
	public GameRoom findBestRoom(int levelID, Player requester, GameRoomManager manager,
			HashMap<GameRoom, Integer> rooms) {
		// Load settings
		int preferredUpperLimit = 75;
		try {
			preferredUpperLimit = Integer
					.parseInt(Centuria.serverProperties.getOrDefault("room-preferred-upper-player-limit", "75"));
		} catch (Exception e) {
		}
		if (requester != null) {
			// Get players that we are following
			SocialEntry[] players = SocialManager.getInstance().getFollowingPlayers(requester.account.getAccountID());
			for (SocialEntry entry : players) {
				CenturiaAccount account = AccountManager.getInstance().getAccount(entry.playerID);
				if (account != null) {
					Player plr = account.getOnlinePlayerInstance();
					if (plr != null) {
						// Check room
						if (plr.levelID == levelID) {
							// Get room if possible
							GameRoom room = manager.getRoom(plr.room);
							if (room != null) {
								// Check amount
								if (room.getPlayers().length < preferredUpperLimit)
									return room; // Found a room
							}
						}
					}
				}
			}
		}
		return null;
	}

}
