package org.asf.centuria.rooms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.rooms.impl.DefaultRoomProvider;
import org.asf.centuria.rooms.impl.GatheringRoomProvider;
import org.asf.centuria.rooms.impl.ModerationRoomProvider;
import org.asf.centuria.rooms.impl.PrivateInstanceRoomProvider;
import org.asf.centuria.rooms.impl.SocialSystemRoomProvider;

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
	private ArrayList<IRoomProvider> roomProviders = new ArrayList<IRoomProvider>();

	public GameRoomManager(GameServer server) {
		this.server = server;

		// Register providers
		// Private instances first, then gatherings, then moderation, then social, then
		// default room providers
		roomProviders.add(new PrivateInstanceRoomProvider(server));
		roomProviders.add(new GatheringRoomProvider());
		roomProviders.add(new ModerationRoomProvider());
		roomProviders.add(new SocialSystemRoomProvider());
		roomProviders.add(new DefaultRoomProvider());
	}

	/**
	 * Registers room providers
	 * 
	 * @param provider Provider to register
	 */
	public void registerRoomProvider(IRoomProvider provider) {
		roomProviders.add(2, provider);
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
		return createRoom(levelID, "", "");
	}

	/**
	 * Allocates empty game rooms
	 * 
	 * @param levelID  Level ID
	 * @param idPrefix ID prefix
	 * @param idSuffix ID suffix
	 * @return GameRoom instance
	 */
	public GameRoom createRoom(int levelID, String idPrefix, String idSuffix) {
		synchronized (roomsByLevel) {
			if (roomsByLevel.containsKey(levelID)) {
				if (roomsByLevel.get(levelID).size() == Integer.MAX_VALUE)
					throw new RuntimeException("Cannot create further rooms, limit reached for this level");
			}
		}

		synchronized (rooms) {
			// Generate instance ID
			String id = idPrefix + Integer.toString(rnd.nextInt(0, Integer.MAX_VALUE), 16) + idSuffix;
			while (getRoom(levelID, id) != null)
				id = idPrefix + Integer.toString(rnd.nextInt(0, Integer.MAX_VALUE), 16) + idSuffix;
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
		// Gather rooms
		HashMap<GameRoom, Integer> rooms = new HashMap<GameRoom, Integer>();
		for (GameRoom room : getAllRooms(levelID)) {
			if (!room.allowSelection)
				continue;
			rooms.put(room, room.getPlayers().length);
		}

		// Go through providers
		for (IRoomProvider prov : roomProviders) {
			// Try to find best room
			GameRoom room = prov.findBestRoom(levelID, requester, this, rooms);
			if (room != null)
				return room;
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
