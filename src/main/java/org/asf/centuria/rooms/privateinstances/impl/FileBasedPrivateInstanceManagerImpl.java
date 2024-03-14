package org.asf.centuria.rooms.privateinstances.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.entities.generic.Quaternion;
import org.asf.centuria.entities.generic.Vector3;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.rooms.ChatRoomTypes;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.rooms.privateinstances.PrivateInstance;
import org.asf.centuria.rooms.privateinstances.PrivateInstanceManager;
import org.asf.centuria.rooms.privateinstances.containervars.PrivateInstanceTeleportVars;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FileBasedPrivateInstanceManagerImpl extends PrivateInstanceManager {

	private GameServer server;
	private File privateInstancesDir;

	protected HashMap<String, FileBasedPrivateInstance> instancesCache = new HashMap<String, FileBasedPrivateInstance>();

	public FileBasedPrivateInstanceManagerImpl(GameServer server) {
		this.server = server;
		privateInstancesDir = new File("privateinstances");
		privateInstancesDir.mkdirs();
	}

	//
	// Private instances
	//

	@Override
	public boolean privateInstanceExists(String id) {
		try {
			return new File(privateInstancesDir, UUID.fromString(id).toString() + ".json").exists();
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public PrivateInstance getPrivateInstance(String id) {
		// Lock
		synchronized (instancesCache) {
			try {
				// Check if present
				if (instancesCache.containsKey(id))
					return instancesCache.get(id);

				// Check file
				if (!privateInstanceExists(id))
					return null;

				// Load
				File file = new File(privateInstancesDir, id + ".json");
				JsonObject data = JsonParser.parseString(Files.readString(file.toPath())).getAsJsonObject();

				// Create container
				FileBasedPrivateInstance privInst = new FileBasedPrivateInstance(this, server.getRoomManager(), file,
						id, data);

				// Cache
				instancesCache.put(id, privInst);

				// Return
				return privInst;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public PrivateInstance createPrivateInstance(String owner, String name, String description) {
		synchronized (instancesCache) {
			try {
				// Build object
				JsonObject instJson = new JsonObject();
				instJson.addProperty("name", name);
				instJson.addProperty("description", description);
				instJson.addProperty("owner", owner);
				instJson.addProperty("allowInvites", true);
				JsonArray parts = new JsonArray();
				parts.add(owner);
				instJson.add("participants", parts);

				// Generate ID
				String id = UUID.randomUUID().toString();
				while (privateInstanceExists(id)) {
					id = UUID.randomUUID().toString();
				}
				File file = new File(privateInstancesDir, id + ".json");

				// Save
				CenturiaAccount ownerA = AccountManager.getInstance().getAccount(owner);
				Centuria.logger.info("Created private instance " + id + " (name: " + name + "), owner: "
						+ (ownerA == null ? " ID " + owner : ownerA.getDisplayName()));
				Files.writeString(file.toPath(), instJson.toString());

				// Create container
				FileBasedPrivateInstance privInst = new FileBasedPrivateInstance(this, server.getRoomManager(), file,
						id, instJson);

				// Add owner
				if (ownerA != null)
					privInst.userJoined(ownerA);

				// Cache
				instancesCache.put(id, privInst);

				// Return
				return privInst;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	//
	// Player
	//

	@Override
	public PrivateInstance[] getJoinedInstancesOf(String participant) {
		CenturiaAccount acc = AccountManager.getInstance().getAccount(participant);
		if (acc != null) {
			// Get joined instances
			ArrayList<PrivateInstance> instances = new ArrayList<PrivateInstance>();

			// Get from storage
			JsonObject instancesLst;
			if (acc.getSaveSharedInventory().containsItem("privateinstances"))
				instancesLst = acc.getSaveSharedInventory().getItem("privateinstances").getAsJsonObject();
			else {
				instancesLst = new JsonObject();
				instancesLst.add("instances", new JsonArray());
			}

			// Get instance list
			JsonArray insts = instancesLst.get("instances").getAsJsonArray();

			// Find all
			for (JsonElement ele : insts) {
				PrivateInstance inst = getPrivateInstance(ele.getAsString());
				if (inst != null)
					instances.add(inst);
			}

			// Return
			return instances.toArray(t -> new PrivateInstance[t]);
		}
		return new PrivateInstance[0];
	}

	@Override
	public PrivateInstance getSelectedInstanceOf(String participant) {
		CenturiaAccount acc = AccountManager.getInstance().getAccount(participant);
		if (acc != null) {
			// Get from storage
			JsonObject instancesLst;
			if (acc.getSaveSharedInventory().containsItem("privateinstances"))
				instancesLst = acc.getSaveSharedInventory().getItem("privateinstances").getAsJsonObject();
			else {
				instancesLst = new JsonObject();
				instancesLst.add("instances", new JsonArray());
			}

			// Find selected
			if (instancesLst.has("selected"))
				return getPrivateInstance(instancesLst.get("selected").getAsString());
		}
		return null;
	}

	@Override
	public void setSelectedInstanceOf(String participant, PrivateInstance instance) {
		CenturiaAccount acc = AccountManager.getInstance().getAccount(participant);
		if (acc != null) {
			// Get from storage
			JsonObject instancesLst;
			if (acc.getSaveSharedInventory().containsItem("privateinstances"))
				instancesLst = acc.getSaveSharedInventory().getItem("privateinstances").getAsJsonObject();
			else {
				instancesLst = new JsonObject();
				instancesLst.add("instances", new JsonArray());
			}

			// Get player
			Player plr = acc.getOnlinePlayerInstance();

			// Check
			if (instance != null) {
				// Set selected if joined
				// Check if this participant has joined
				if (instance.isParticipant(participant)) {
					// Set selected instance
					instancesLst.addProperty("selected", instance.getID());

					// Save
					acc.getSaveSharedInventory().setItem("privateinstances", instancesLst);

					// Teleport if ingame
					if (plr != null && (plr.getRoom() != null || plr.room != null)) {
						// Teleport if ready
						if (plr.roomReady
								|| (plr.room != null && plr.pendingRoom != null && plr.pendingRoom.equals(plr.room))) {
							// Teleport to room
							String roomID = instance.getRoom(plr.levelID).getID();
							plr.targetPos = new Vector3(plr.lastPos.x, plr.lastPos.y, plr.lastPos.z);
							plr.targetRot = new Quaternion(plr.lastRot.x, plr.lastRot.y, plr.lastRot.z, plr.lastRot.w);
							plr.teleportToRoom(plr.levelID, plr.levelType, 0, roomID, "");
						} else if (plr.room == null
								|| (plr.room != null && plr.pendingRoom != null && !plr.pendingRoom.equals(plr.room))) {
							// Update pending room
							String pending = plr.pendingRoom;
							String newRoom = instance.getRoom(plr.pendingLevelID).getID();
							plr.pendingRoom = newRoom;

							// Update chat client just in case
							ChatClient chClient = Centuria.chatServer.getClient(plr.account.getAccountID());
							if (chClient != null) {
								// Leave old room
								if (chClient.isInRoom(pending))
									chClient.leaveRoom(pending);

								// Join room
								if (!chClient.isInRoom(newRoom))
									chClient.joinRoom(newRoom, ChatRoomTypes.ROOM_CHAT);
							}
						}
					}
				}
			} else {
				// Disconnect

				// Remove selected instance
				if (instancesLst.has("selected"))
					instancesLst.remove("selected");

				// Save
				acc.getSaveSharedInventory().setItem("privateinstances", instancesLst);

				// Teleport if ingame
				if (plr != null && (plr.getRoom() != null || plr.room != null)) {
					// Teleport if ready
					if (plr.roomReady
							|| (plr.room != null && plr.pendingRoom != null && plr.pendingRoom.equals(plr.room))) {
						// Teleport to room
						String roomID = server.getRoomManager().findBestRoom(plr.levelID, plr).getID();
						plr.targetPos = new Vector3(plr.lastPos.x, plr.lastPos.y, plr.lastPos.z);
						plr.targetRot = new Quaternion(plr.lastRot.x, plr.lastRot.y, plr.lastRot.z, plr.lastRot.w);
						plr.teleportToRoom(plr.levelID, plr.levelType, 0, roomID, "");
					} else if (plr.room == null
							|| (plr.room != null && plr.pendingRoom != null && !plr.pendingRoom.equals(plr.room))) {
						// Update pending room
						String pending = plr.pendingRoom;
						String newRoom = server.getRoomManager().findBestRoom(plr.pendingLevelID, plr).getID();
						plr.pendingRoom = newRoom;

						// Update chat client just in case
						ChatClient chClient = Centuria.chatServer.getClient(plr.account.getAccountID());
						if (chClient != null) {
							// Leave old room
							if (chClient.isInRoom(pending))
								chClient.leaveRoom(pending);

							// Join room
							if (!chClient.isInRoom(newRoom))
								chClient.joinRoom(newRoom, ChatRoomTypes.ROOM_CHAT);
						}
					}
				}
			}

			// Update variables
			if (plr != null) {
				PrivateInstanceTeleportVars vars = plr.getObject(PrivateInstanceTeleportVars.class);
				if (vars == null) {
					// Create
					vars = new PrivateInstanceTeleportVars();
					plr.addObject(vars);
				}
				vars.forcefullyConnectedToInstance = instance != null;
			}
		}
	}

}
