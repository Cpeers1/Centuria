package org.asf.emuferal.packets.xt.gameserver.world;

import java.io.IOException;
import java.io.InputStream;

import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.entities.generic.Velocity;
import org.asf.emuferal.entities.objects.WorldObjectMoveNodeData;
import org.asf.emuferal.entities.objects.WorldObjectPositionInfo;
import org.asf.emuferal.entities.sanctuaries.SanctuaryObjectData;
import org.asf.emuferal.enums.actors.ActorActionType;
import org.asf.emuferal.enums.sanctuaries.SanctuaryObjectType;
import org.asf.emuferal.interactions.InteractionManager;
import org.asf.emuferal.modules.eventbus.EventBus;
import org.asf.emuferal.modules.events.levels.LevelJoinEvent;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.emuferal.packets.xt.gameserver.sanctuaries.SanctuaryWorldObjectInfo;
import org.asf.emuferal.players.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WorldReadyPacket implements IXtPacket<WorldReadyPacket> {

	public String teleportUUID = "";

	@Override
	public WorldReadyPacket instantiate() {
		return new WorldReadyPacket();
	}

	@Override
	public String id() {
		return "wr";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		teleportUUID = reader.read();

		if (System.getProperty("debugMode") != null) {
			System.out.println("[WorldReadyPacket] recieved...");
		}
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

		// Override teleport
		if (plr.teleportDestination != null) {
			teleportUUID = plr.teleportDestination;
			plr.teleportDestination = null;
		}

		// Initialize interaction memory
		plr.account.getPlayerInventory().getInteractionMemory().prepareLevel(plr.pendingLevelID);

		// Initialize interactions
		InteractionManager.initInteractionsFor(client, plr.pendingLevelID);

		// Save changes
		plr.account.getPlayerInventory().getInteractionMemory().saveTo(client);

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new LevelJoinEvent(plr.pendingLevelID, plr.pendingRoom, plr));

		// Send to tutorial if new
		if (plr.account.isPlayerNew()) {
			// Tutorial spawn
			WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
			res.x = 107.67;
			res.y = 8.85;
			res.z = -44.85;
			res.rx = 0;
			res.ry = 0.9171;
			res.rz = -0;
			res.rw = 0.3987;
			client.sendPacket(res);
			return true;
		}

		// Sync
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				plr.destroyAt(player);
			}
		}

		// Assign info
		plr.room = plr.pendingRoom;
		plr.levelID = plr.pendingLevelID;

		// Send all other players to the current player
		GameServer server = (GameServer) client.getServer();
		for (Player player : server.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				player.syncTo(plr);

				if (System.getProperty("debugMode") != null) {
					System.out.println("[WorldReadyPacket] Syncing player " + player.account.getDisplayName() + " to "
							+ plr.account.getDisplayName());
				}

			}
		}

		try {
			Thread.sleep(5000); // Temporary wait
		} catch (InterruptedException e) {
		}

		// Find spawn
		handleSpawn(teleportUUID, plr, client);

		// Sync spawn
		for (Player player : server.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				plr.syncTo(player);

				if (System.getProperty("debugMode") != null) {
					System.out.println("[WorldReadyPacket] Syncing spawn " + player.account.getDisplayName() + " to "
							+ plr.account.getDisplayName());
				}
			}
		}

		// Set location
		plr.lastLocation = plr.respawn;

		// Sanctuary loading
		if (plr.levelType == 2 && plr.room.startsWith("sanctuary_")) {
			String ownerID = plr.room.substring("sanctuary_".length());

			// Find account
			EmuFeralAccount acc = AccountManager.getInstance().getAccount(ownerID);

			// Find active sanctuary object
			JsonObject sanctuaryInfo = acc.getPlayerInventory().getSanctuaryAccessor()
					.getSanctuaryLook(acc.getActiveSanctuaryLook());
			if (sanctuaryInfo == null)
				sanctuaryInfo = acc.getPlayerInventory().getSanctuaryAccessor().getFirstSanctuaryLook();

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
			loadSanctuary(id, info, plr, acc, acc.getPlayerInventory(), client);
		}

		// Mark as ready (for teleports etc)
		plr.roomReady = true;

		return true;
	}

	private void loadSanctuary(String id, JsonObject info, Player player, EmuFeralAccount acc, PlayerInventory inv,
			SmartfoxClient client) {
		// Load furniture
		JsonObject placementInfo = info.get("placementInfo").getAsJsonObject();
		if (placementInfo.has("items")) {
			JsonArray items = placementInfo.get("items").getAsJsonArray();
			for (JsonElement ele : items) {
				JsonObject furnitureInfo = ele.getAsJsonObject().get("components").getAsJsonObject().get("Placed")
						.getAsJsonObject();

				String objId = furnitureInfo.get("placeableInvId").getAsString();
				JsonObject furnitureObject = inv.getFurnitureAccessor().getFurnitureData(objId);
				if (furnitureObject != null) {
					// Send packet

					SanctuaryWorldObjectInfo sanctuaryWorldObjectInfo = new SanctuaryWorldObjectInfo();
					sanctuaryWorldObjectInfo.id = objId; // World object ID
					sanctuaryWorldObjectInfo.defId = 1751;
					sanctuaryWorldObjectInfo.ownerId = player.room.substring("sanctuary_".length());

					var positionInfo = new WorldObjectPositionInfo(
						furnitureInfo.get("xPos").getAsDouble(), furnitureInfo.get("yPos").getAsDouble(),
						furnitureInfo.get("zPos").getAsDouble(), furnitureInfo.get("rotX").getAsDouble(),
						furnitureInfo.get("rotY").getAsDouble(), furnitureInfo.get("rotZ").getAsDouble(),
						furnitureInfo.get("rotW").getAsDouble()
					);
					
					// Object info
					sanctuaryWorldObjectInfo.lastMove = new WorldObjectMoveNodeData();
					sanctuaryWorldObjectInfo.lastMove.actorActionType = ActorActionType.None;
					sanctuaryWorldObjectInfo.lastMove.serverTime = System.currentTimeMillis() / 1000;
					sanctuaryWorldObjectInfo.lastMove.positionInfo = positionInfo;
					sanctuaryWorldObjectInfo.lastMove.velocity = new Velocity();
					
					//Sanc Object Info
					sanctuaryWorldObjectInfo.objectType = SanctuaryObjectType.Furniture;
					sanctuaryWorldObjectInfo.funitureObject = furnitureObject;

					// Only send json if its not the owner
					if (!player.account.getAccountID().equals(acc.getAccountID()))
						sanctuaryWorldObjectInfo.writeFurnitureInfo = true;

					sanctuaryWorldObjectInfo.sancObjectInfo = new SanctuaryObjectData(positionInfo, furnitureInfo.get("gridId").getAsInt(), furnitureInfo.get("parentItemId").getAsString(), furnitureInfo.get("state").getAsInt())
						
					client.sendPacket(sanctuaryWorldObjectInfo);

					// Log
					if (System.getProperty("debugMode") != null) {
						System.out.println("[SANCTUARY] [LOAD]  Server to client: load object (id: " + objId
								+ ", type: furniture, defId: " + furnitureObject.get("defId").getAsString() + ")");
					}
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
		if (System.getProperty("debugMode") != null) {
			System.out.println("[SANCTUARY] [LOAD]  Server to client: load object (id: " + houseId
					+ ", type: house, defId: " + houseJson.get("defId").getAsString() + ")");
		}

		// Load island info
		String islandId = info.get("islandInvId").getAsString();
		JsonObject islandJson = inv.getSanctuaryAccessor().getIslandTypeObject(islandId);

		// Send packet
		client.sendPacket("%xt%oi%-1%" + islandId + "%1751%" + player.room.substring("sanctuary_".length()) + "%0%"
				+ (System.currentTimeMillis() / 1000) + "%0%0%0%0%0%0%1%0%0%0%0.0%0%1%" + islandJson.toString() + "%");

		// Log
		if (System.getProperty("debugMode") != null) {
			System.out.println("[SANCTUARY] [LOAD]  Server to client: load object (id: " + islandId
					+ ", type: island, defId: " + islandJson.get("defId").getAsString() + ")");
		}
	}

	private void handleSpawn(String id, Player plr, SmartfoxClient client) throws IOException {
		// Find teleport

		// First attempt to find a player with the ID
		for (Player player : ((GameServer) client.getServer()).getPlayers()) {
			if (player.account.getAccountID().equals(id)) {
				// Send response
				System.out.println(
						"Player teleport: " + plr.account.getDisplayName() + ": " + player.account.getDisplayName());
				WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
				res.x = player.lastPosX;
				res.y = player.lastPosY;
				res.z = player.lastPosZ;
				res.rw = player.lastRotW;
				res.rx = player.lastRotX;
				res.ry = player.lastRotY;
				res.rz = player.lastRotZ;
				plr.lastPosX = res.x;
				plr.lastPosY = res.y;
				plr.lastPosZ = res.z;
				plr.lastRotW = res.rx;
				plr.lastRotX = res.ry;
				plr.lastRotY = res.rz;
				plr.lastRotZ = res.rw;
				client.sendPacket(res);
				plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rx + "%" + res.ry + "%" + res.rz + "%"
						+ res.rw;
				return;
			}
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
				System.out.println("Player teleport: " + plr.account.getDisplayName() + ": "
						+ helper.get("worldID").getAsString());
				WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
				res.x = helper.get("spawnX").getAsDouble();
				res.y = helper.get("spawnY").getAsDouble();
				res.z = helper.get("spawnZ").getAsDouble();
				res.rw = helper.get("spawnRotW").getAsDouble();
				res.rx = helper.get("spawnRotX").getAsDouble();
				res.ry = helper.get("spawnRotY").getAsDouble();
				res.rz = helper.get("spawnRotZ").getAsDouble();
				plr.lastPosX = res.x;
				plr.lastPosY = res.y;
				plr.lastPosZ = res.z;
				plr.lastRotW = res.rx;
				plr.lastRotX = res.ry;
				plr.lastRotY = res.rz;
				plr.lastRotZ = res.rw;
				client.sendPacket(res);
				plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rx + "%" + res.ry + "%" + res.rz + "%"
						+ res.rw;
				return;
			}
		} catch (IOException e) {
		}

		// Spawn not found
		System.err.println("Player teleport: " + plr.account.getDisplayName() + " to unrecognized spawn!");
		WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
		res.x = 0;
		res.y = 80;
		res.z = 0;
		res.rw = 0;
		res.rx = 0;
		res.ry = 0;
		res.rz = 0;
		plr.lastPosX = res.x;
		plr.lastPosY = res.y;
		plr.lastPosZ = res.z;
		plr.lastRotW = res.rw;
		plr.lastRotX = res.rx;
		plr.lastRotY = res.ry;
		plr.lastRotZ = res.rz;
		client.sendPacket(res);
		plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rx + "%" + res.ry + "%" + res.rz + "%" + res.rw;
	}

}
