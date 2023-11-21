package org.asf.centuria.packets.xt.gameserver.world;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.generic.Velocity;
import org.asf.centuria.entities.objects.WorldObjectMoveNodeData;
import org.asf.centuria.entities.objects.WorldObjectPositionInfo;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.entities.sanctuaries.SanctuaryObjectData;
import org.asf.centuria.enums.objects.WorldObjectMoverNodeType;
import org.asf.centuria.enums.sanctuaries.SanctuaryObjectType;
import org.asf.centuria.interactions.InteractionManager;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.levels.LevelJoinEvent;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.rooms.ChatRoomTypes;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.centuria.packets.xt.gameserver.object.ObjectInfoAvatarLocalPacket;
import org.asf.centuria.packets.xt.gameserver.sanctuary.SanctuaryWorldObjectInfoPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WorldReadyPacket implements IXtPacket<WorldReadyPacket> {

	private static final String PACKET_ID = "wr";

	public String teleportUUID = "";

	@Override
	public WorldReadyPacket instantiate() {
		return new WorldReadyPacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		teleportUUID = reader.read();
		Centuria.logger.debug(MarkerManager.getMarker("WorldReadyPacket"), "recieved...");
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Find the world coordinates

		// Load player
		Player plr = (Player) client.container;
		plr.respawnItems.clear();
		plr.disableSync = false;

		// Override teleport
		if (plr.teleportDestination != null) {
			teleportUUID = plr.teleportDestination;
			plr.teleportDestination = null;
		}

		// Remove players
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (player.room != null && plr.room != null && player.room.equals(plr.room) && player != plr) {
				plr.destroyAt(player);
			}
		}

		// Remove players
		for (Player player : srv.getPlayers()) {
			if (player.room != null && plr.room != null && player.room.equals(plr.room) && player != plr) {
				player.destroyAt(plr);
			}
		}

		// Initialize interaction memory
		plr.account.getSaveSpecificInventory().getInteractionMemory().prepareLevel(plr.pendingLevelID);

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new LevelJoinEvent(plr.pendingLevelID, plr.pendingRoom, plr));

		// Assign info
		plr.room = plr.pendingRoom;
		plr.levelID = plr.pendingLevelID;

		// Minigame sync
		if (plr.comingFromMinigame) {
			plr.comingFromMinigame = false;
			plr.targetPos = plr.lastPos;
			plr.targetRot = plr.lastRot;
		}

		// Send all other players to the current player
		GameServer server = (GameServer) client.getServer();
		for (Player player : server.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				player.syncTo(plr, WorldObjectMoverNodeType.InitPosition);
				Centuria.logger.debug(MarkerManager.getMarker("WorldReadyPacket"),
						"Syncing player " + player.account.getDisplayName() + " to " + plr.account.getDisplayName());
			}
		}

		// Send to tutorial if new
		if (plr.account.isPlayerNew()) {
			// XP init
			if (plr.account.getLevel().isLevelAvailable() && !plr.account.isPlayerNew())
				plr.account.getLevel().onWorldJoin(plr);

			// Tutorial spawn
			ObjectInfoAvatarLocalPacket res = new ObjectInfoAvatarLocalPacket();
			res.x = 107.67;
			res.y = 8.85;
			res.z = -44.85;
			res.rx = 0;
			res.ry = 0.9171;
			res.rz = -0;
			res.rw = 0.3987;
			client.sendPacket(res);

			// Initialize interactions
			InteractionManager.initInteractionsFor(plr, plr.pendingLevelID);

			// Sync spawn
			for (Player player : server.getPlayers()) {
				if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
					plr.syncTo(player, WorldObjectMoverNodeType.InitPosition);
					Centuria.logger.debug(MarkerManager.getMarker("WorldReadyPacket"),
							"Syncing spawn " + player.account.getDisplayName() + " to " + plr.account.getDisplayName());
				}
			}

			return true;
		}

		// Save changes
		plr.account.getSaveSpecificInventory().getInteractionMemory().saveTo(client);

		// XP init
		if (plr.account.getLevel().isLevelAvailable() && !plr.account.isPlayerNew())
			plr.account.getLevel().onWorldJoin(plr);

		// If there is a chat server connection, switch the chat to the new room to get
		// around the chat room leave bug which causes players to see chat from other
		// worlds
		ChatClient chClient = Centuria.chatServer.getClient(plr.account.getAccountID());
		if (chClient != null) {
			// Leave old public rooms
			for (String room : chClient.getRooms()) {
				if (chClient.getRoom(room).getType().equalsIgnoreCase(ChatRoomTypes.ROOM_CHAT))
					chClient.leaveRoom(room);
			}

			// Join room
			if (!chClient.isInRoom(plr.room))
				chClient.joinRoom(plr.room, ChatRoomTypes.ROOM_CHAT);

			// Make the player know its in the chat
			plr.wasInChat = true;
		}

		// Find spawn
		handleSpawn(teleportUUID, plr, client);

		// Initialize interactions
		InteractionManager.initInteractionsFor(plr, plr.pendingLevelID);

		// Reset target
		plr.targetPos = null;
		plr.targetRot = null;

		// Sync spawn
		for (Player player : server.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				plr.syncTo(player, WorldObjectMoverNodeType.InitPosition);
				Centuria.logger.debug(MarkerManager.getMarker("WorldReadyPacket"),
						"Syncing spawn " + player.account.getDisplayName() + " to " + plr.account.getDisplayName());
			}
		}

		// Sanctuary loading
		if (plr.levelType == 2 && plr.room.startsWith("sanctuary_")) {
			String ownerID = plr.room.substring("sanctuary_".length());

			// Find account
			CenturiaAccount acc = AccountManager.getInstance().getAccount(ownerID);

			// Find active sanctuary object
			JsonObject sanctuaryInfo = acc.getSaveSpecificInventory().getSanctuaryAccessor()
					.getSanctuaryLook(acc.getActiveSanctuaryLook());
			if (sanctuaryInfo == null)
				sanctuaryInfo = acc.getSaveSpecificInventory().getSanctuaryAccessor().getFirstSanctuaryLook();

			// Find the ID
			String id = sanctuaryInfo.get("id").getAsString();
			if (acc.getAccountID().equals(plr.account.getAccountID()) && !plr.activeSanctuaryLook.equals(id)) {
				plr.activeSanctuaryLook = id;
				plr.account.setActiveSanctuaryLook(plr.activeSanctuaryLook);
			}

			// Find sanctuary info
			JsonObject info = sanctuaryInfo.get("components").getAsJsonObject().get("SanctuaryLook").getAsJsonObject()
					.get("info").getAsJsonObject();

			// Load sanctuary
			loadSanctuary(id, info, plr, acc, acc.getSaveSpecificInventory(), client);
		}

		// Mark as ready (for teleports etc)
		plr.roomReady = true;

		return true;
	}

	private void loadSanctuary(String id, JsonObject info, Player player, CenturiaAccount acc, PlayerInventory inv,
			SmartfoxClient client) {
		// Load furniture
		JsonObject placementInfo = info.get("placementInfo").getAsJsonObject();
		if (placementInfo.has("items")) {
			JsonArray items = placementInfo.get("items").getAsJsonArray();
			ArrayList<JsonElement> elements = new ArrayList<JsonElement>();
			for (JsonElement ele : items) {
				elements.add(ele);
			}
			for (JsonElement ele : elements) {
				JsonObject furnitureInfo = ele.getAsJsonObject().get("components").getAsJsonObject().get("Placed")
						.getAsJsonObject();

				String objId = furnitureInfo.get("placeableInvId").getAsString();
				JsonObject furnitureObject = inv.getFurnitureAccessor().getFurnitureData(objId);
				if (furnitureObject != null) {
					// Send packet

					SanctuaryWorldObjectInfoPacket sanctuaryWorldObjectInfo = new SanctuaryWorldObjectInfoPacket();
					sanctuaryWorldObjectInfo.id = objId; // World object ID
					sanctuaryWorldObjectInfo.defId = 1751;
					sanctuaryWorldObjectInfo.ownerId = player.room.substring("sanctuary_".length());

					var positionInfo = new WorldObjectPositionInfo(furnitureInfo.get("xPos").getAsDouble(),
							furnitureInfo.get("yPos").getAsDouble(), furnitureInfo.get("zPos").getAsDouble(),
							furnitureInfo.get("rotX").getAsDouble(), furnitureInfo.get("rotY").getAsDouble(),
							furnitureInfo.get("rotZ").getAsDouble(), furnitureInfo.get("rotW").getAsDouble());

					// Object info
					sanctuaryWorldObjectInfo.lastMove = new WorldObjectMoveNodeData();
					sanctuaryWorldObjectInfo.lastMove.actorActionType = 0;
					sanctuaryWorldObjectInfo.lastMove.serverTime = System.currentTimeMillis() / 1000;
					sanctuaryWorldObjectInfo.lastMove.positionInfo = positionInfo;
					sanctuaryWorldObjectInfo.lastMove.velocity = new Velocity();

					// Sanc Object Info
					sanctuaryWorldObjectInfo.objectType = SanctuaryObjectType.Furniture;
					sanctuaryWorldObjectInfo.funitureObject = furnitureObject;

					// Only send json if its not the owner
					if (!player.account.getAccountID().equals(acc.getAccountID()))
						sanctuaryWorldObjectInfo.writeFurnitureInfo = true;

					sanctuaryWorldObjectInfo.sancObjectInfo = new SanctuaryObjectData(positionInfo,
							furnitureInfo.get("gridId").getAsInt(), furnitureInfo.get("parentItemId").getAsString(),
							furnitureInfo.get("state").getAsInt());

					client.sendPacket(sanctuaryWorldObjectInfo);

					// Log
					Centuria.logger.debug(MarkerManager.getMarker("SANCTUARY"),
							"[LOAD]  Server to client: load object (id: " + objId + ", type: furniture, defId: "
									+ furnitureObject.get("defId").getAsString() + ")");
				} else {
					// Remove it
					Centuria.logger.debug(MarkerManager.getMarker("SANCTUARY"),
							"[LOAD]  Server to client: could not load object (id: " + objId
									+ "): not in inventory, removing it...");
					items.remove(ele.getAsJsonObject());
				}
			}
			if (items.size() != elements.size()) {
				// Save
				inv.setItem("201", inv.getItem("201"));

				// Send to client
				Player oPlr = acc.getOnlinePlayerInstance();
				if (oPlr != null) {
					JsonArray arr = new JsonArray();
					JsonObject sanctuaryInfo = acc.getSaveSpecificInventory().getSanctuaryAccessor()
							.getSanctuaryLook(acc.getActiveSanctuaryLook());
					if (sanctuaryInfo == null)
						sanctuaryInfo = acc.getSaveSpecificInventory().getSanctuaryAccessor().getFirstSanctuaryLook();
					arr.add(sanctuaryInfo);
					InventoryItemPacket packet = new InventoryItemPacket();
					packet.item = arr;
					oPlr.client.sendPacket(packet);
				}
			}
		}

		// Load house info
		String houseId = info.get("houseInvId").getAsString();
		JsonObject houseJson = inv.getSanctuaryAccessor().getHouseTypeObject(houseId);

		// Send packet
		client.sendPacket("%xt%oi%-1%" + houseId + "%1751%" + player.room.substring("sanctuary_".length()) + "%0%"
				+ (System.currentTimeMillis() / 1000) + "%0%0%0%0%0%0%1%0%0%0%0.0%0%0%" + houseJson.toString() + "%");

		// Log
		Centuria.logger.debug(MarkerManager.getMarker("SANCTUARY"), "[LOAD]  Server to client: load object (id: "
				+ houseId + ", type: house, defId: " + houseJson.get("defId").getAsString() + ")");

		// Load island info
		String islandId = info.get("islandInvId").getAsString();
		JsonObject islandJson = inv.getSanctuaryAccessor().getIslandTypeObject(islandId);

		// Send packet
		client.sendPacket("%xt%oi%-1%" + islandId + "%1751%" + player.room.substring("sanctuary_".length()) + "%0%"
				+ (System.currentTimeMillis() / 1000) + "%0%0%0%0%0%0%1%0%0%0%0.0%0%1%" + islandJson.toString() + "%");

		// Log
		Centuria.logger.debug(MarkerManager.getMarker("SANCTUARY"), "[LOAD]  Server to client: load object (id: "
				+ islandId + ", type: island, defId: " + islandJson.get("defId").getAsString() + ")");
	}

	private void handleSpawn(String id, Player plr, SmartfoxClient client) throws IOException {
		// Find teleport

		// First attempt to find a player with the ID
		for (Player player : ((GameServer) client.getServer()).getPlayers()) {
			if (player.account.getAccountID().equals(id)) {
				// Send response
				Centuria.logger.info(
						"Player teleport: " + plr.account.getDisplayName() + ": " + player.account.getDisplayName());

				// Check room
				if (player.levelID != plr.pendingLevelID) {
					continue;
				}

				ObjectInfoAvatarLocalPacket res = new ObjectInfoAvatarLocalPacket();
				res.x = player.lastPos.x;
				res.y = player.lastPos.y;
				res.z = player.lastPos.z;
				res.rw = player.lastRot.w;
				res.rx = player.lastRot.x;
				res.ry = player.lastRot.y;
				res.rz = player.lastRot.z;
				plr.lastPos.x = res.x;
				plr.lastPos.y = res.y;
				plr.lastPos.z = res.z;
				plr.lastRot.w = res.rx;
				plr.lastRot.x = res.ry;
				plr.lastRot.y = res.rz;
				plr.lastRot.z = res.rw;
				client.sendPacket(res);
				plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rx + "%" + res.ry + "%" + res.rz + "%"
						+ res.rw;
				return;
			}
		}

		// Spawn at target if present
		if (plr.targetPos != null && plr.targetRot != null) {
			ObjectInfoAvatarLocalPacket res = new ObjectInfoAvatarLocalPacket();
			res.x = plr.targetPos.x;
			res.y = plr.targetPos.y;
			res.z = plr.targetPos.z;
			res.rw = plr.targetRot.w;
			res.rx = plr.targetRot.x;
			res.ry = plr.targetRot.y;
			res.rz = plr.targetRot.z;
			plr.lastPos.x = res.x;
			plr.lastPos.y = res.y;
			plr.lastPos.z = res.z;
			plr.lastRot.w = res.rx;
			plr.lastRot.x = res.ry;
			plr.lastRot.y = res.rz;
			plr.lastRot.z = res.rw;
			client.sendPacket(res);
			plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rx + "%" + res.ry + "%" + res.rz + "%" + res.rw;
			return;
		}

		// Load spawn helper
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader().getResourceAsStream("spawns.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Spawns").getAsJsonObject();
			strm.close();

			// Check existence
			if (helper.has(plr.pendingLevelID + "/" + id)) {
				// Send response
				helper = helper.get(plr.pendingLevelID + "/" + id).getAsJsonObject();
				Centuria.logger.info("Player teleport: " + plr.account.getDisplayName() + ": "
						+ helper.get("worldID").getAsString());

				ObjectInfoAvatarLocalPacket res = new ObjectInfoAvatarLocalPacket();
				res.x = helper.get("spawnX").getAsDouble();
				res.y = helper.get("spawnY").getAsDouble();
				res.z = helper.get("spawnZ").getAsDouble();
				res.rw = helper.get("spawnRotW").getAsDouble();
				res.rx = helper.get("spawnRotX").getAsDouble();
				res.ry = helper.get("spawnRotY").getAsDouble();
				res.rz = helper.get("spawnRotZ").getAsDouble();
				plr.lastPos.x = res.x;
				plr.lastPos.y = res.y;
				plr.lastPos.z = res.z;
				plr.lastRot.w = res.rx;
				plr.lastRot.x = res.ry;
				plr.lastRot.y = res.rz;
				plr.lastRot.z = res.rw;
				client.sendPacket(res);
				plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rx + "%" + res.ry + "%" + res.rz + "%"
						+ res.rw;
				return;
			}
		} catch (IOException e) {
		}

		// Spawn not found
		Centuria.logger.info("Player teleport: " + plr.account.getDisplayName() + " to unrecognized spawn!");
		ObjectInfoAvatarLocalPacket res = new ObjectInfoAvatarLocalPacket();
		res.x = 0;
		res.y = 80;
		res.z = 0;
		res.rw = 0;
		res.rx = 0;
		res.ry = 0;
		res.rz = 0;
		plr.lastPos.x = res.x;
		plr.lastPos.y = res.y;
		plr.lastPos.z = res.z;
		plr.lastRot.w = res.rw;
		plr.lastRot.x = res.rx;
		plr.lastRot.y = res.ry;
		plr.lastRot.z = res.rz;
		client.sendPacket(res);
		plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rx + "%" + res.ry + "%" + res.rz + "%" + res.rw;
	}

}
