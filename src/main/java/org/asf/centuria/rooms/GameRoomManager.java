package org.asf.centuria.rooms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.social.SocialEntry;
import org.asf.centuria.social.SocialManager;

/**
 * 
 * Game Room Manager
 * 
 * @author Sky Swimmer
 * 
 */
public class GameRoomManager {

	private GameServer server;
	private static Random rnd = new Random();
	private HashMap<String, GameRoom> rooms = new HashMap<String, GameRoom>();
	private HashMap<Integer, ArrayList<GameRoom>> roomsByLevel = new HashMap<Integer, ArrayList<GameRoom>>();

	public GameRoomManager(GameServer server) {
		this.server = server;
	}

	/**
	 * Retrieves rooms by level and instance IDs
	 * 
	 * @param levelID    Level ID
	 * @param instanceID Room instance ID
	 * @return GameRoom instance or null
	 */
	public GameRoom getRoom(int levelID, String instanceID) {
		synchronized (roomsByLevel) {
			if (roomsByLevel.containsKey(levelID)) {
				Optional<GameRoom> opt = roomsByLevel.get(levelID).stream()
						.filter(t -> t.getInstanceID().equalsIgnoreCase(instanceID)).findFirst();
				if (opt.isPresent())
					return opt.get();
			}
			return null;
		}
	}

	/**
	 * Retrieves rooms by ID
	 * 
	 * @param id Room ID
	 * @return GameRoom instance or null
	 */
	public GameRoom getRoom(String id) {
		synchronized (rooms) {
			return rooms.get(id);
		}
	}

	/**
	 * Retrieves all known rooms
	 * 
	 * @return Array of GameRoom instances
	 */
	public GameRoom[] getAllRooms() {
		synchronized (rooms) {
			return rooms.values().toArray(t -> new GameRoom[t]);
		}
	}

	/**
	 * Retrieves all known rooms by level ID
	 * 
	 * @param levelID Level ID
	 * @return Array of GameRoom instances
	 */
	public GameRoom[] getAllRooms(int levelID) {
		synchronized (roomsByLevel) {
			if (roomsByLevel.containsKey(levelID))
				return roomsByLevel.get(levelID).toArray(t -> new GameRoom[t]);
		}
		return new GameRoom[] { getOrCreateRoom(levelID, "STAFFROOM") };
	}

	/**
	 * Allocates empty game rooms
	 * 
	 * @param levelID Level ID
	 * @return GameRoom instance
	 */
	public GameRoom createRoom(int levelID) {
		synchronized (roomsByLevel) {
			if (roomsByLevel.containsKey(levelID)) {
				if (roomsByLevel.get(levelID).size() == Integer.MAX_VALUE)
					throw new RuntimeException("Cannot create further rooms, limit reached for this level");
			}
		}

		synchronized (rooms) {
			// Generate instance ID
			String id = Integer.toString(rnd.nextInt(0, Integer.MAX_VALUE), 16);
			while (getRoom(levelID, id) != null)
				id = Integer.toString(rnd.nextInt(0, Integer.MAX_VALUE), 16);
			return createRoomInt(levelID, id);
		}
	}

	/**
	 * Finds or creates game rooms by level ID and instance ID
	 * 
	 * @param levelID    Level ID
	 * @param instanceID Room instance ID
	 * @return GameRoom instance
	 */
	public GameRoom getOrCreateRoom(int levelID, String instanceID) {
		synchronized (roomsByLevel) {
			if (roomsByLevel.containsKey(levelID)) {
				if (roomsByLevel.get(levelID).size() == Integer.MAX_VALUE)
					throw new RuntimeException("Cannot create further rooms, limit reached for this level");
			}
		}

		synchronized (rooms) {
			return createRoomInt(levelID, instanceID);
		}
	}

	/**
	 * Finds the best room for a player
	 * 
	 * @param levelID   Level ID
	 * @param requester Requesting player
	 * @return GameRoom instance
	 */
	public GameRoom findBestRoom(int levelID, Player requester) {
		// Load settings
		int preferredMin = 15;
		int preferredMax = 50;
		int preferredUpperLimit = 75;
		try {
			preferredMin = Integer.parseInt(Centuria.serverProperties.getOrDefault("room-preferred-min-players", "15"));
		} catch (Exception e) {
		}
		try {
			preferredMax = Integer.parseInt(Centuria.serverProperties.getOrDefault("room-preferred-max-players", "50"));
		} catch (Exception e) {
		}
		try {
			preferredUpperLimit = Integer
					.parseInt(Centuria.serverProperties.getOrDefault("room-preferred-upper-player-limit", "75"));
		} catch (Exception e) {
		}

		// Check social list
		HashMap<GameRoom, Integer> rooms = new HashMap<GameRoom, Integer>();
		for (GameRoom room : getAllRooms(levelID)) {
			if (!room.allowSelection)
				continue;
			rooms.put(room, room.getPlayers().length);
		}
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
							GameRoom room = getRoom(plr.room);
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

		// Create new room
		GameRoom room = createRoom(levelID);
		room.allowSelection = true;
		return room;
	}

	private GameRoom createRoomInt(int levelID, String instanceID) {
		// Verify instance ID
		GameRoom roomOld = getRoom(levelID, instanceID);
		if (roomOld != null)
			return roomOld;

		// Generate ID
		String id = UUID.randomUUID().toString();
		while (rooms.containsKey("room_" + instanceID + "_" + id))
			id = UUID.randomUUID().toString();

		// Create room
		GameRoom room = new GameRoom("room_" + instanceID + "_" + id, instanceID, levelID, server);

		// Add
		addRoom(room);

		// Return
		return room;
	}

	private void addRoom(GameRoom room) {
		rooms.put(room.getID(), room);
		synchronized (roomsByLevel) {
			if (!roomsByLevel.containsKey(room.getLevelID())) {
				if (room.getInstanceID().equalsIgnoreCase("STAFFROOM"))
					roomsByLevel.put(room.getLevelID(), new ArrayList<GameRoom>());
			}
			roomsByLevel.get(room.getLevelID()).add(room);
		}

		if (!room.getInstanceID().equalsIgnoreCase("STAFFROOM"))
			createRoomInt(room.getLevelID(), "STAFFROOM");
	}
}
