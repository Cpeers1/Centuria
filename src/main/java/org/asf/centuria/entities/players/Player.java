package org.asf.centuria.entities.players;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;

import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.generic.Quaternion;
import org.asf.centuria.entities.generic.Vector3;
import org.asf.centuria.entities.generic.Velocity;
import org.asf.centuria.entities.inventoryitems.InventoryItem;
import org.asf.centuria.entities.objects.WorldObjectMoveNodeData;
import org.asf.centuria.entities.objects.WorldObjectPositionInfo;
import org.asf.centuria.entities.trading.Trade;
import org.asf.centuria.entities.uservars.UserVarValue;
import org.asf.centuria.enums.actors.ActorActionType;
import org.asf.centuria.enums.objects.WorldObjectMoverNodeType;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.gameserver.objects.WorldObjectDelete;
import org.asf.centuria.packets.xt.gameserver.players.PlayerWorldObjectInfo;
import org.asf.centuria.packets.xt.gameserver.social.JumpToPlayer;
import org.asf.centuria.packets.xt.gameserver.world.JoinRoom;
import org.asf.centuria.social.SocialManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Player {

	public SmartfoxClient client;
	public CenturiaAccount account;

	public String activeLook;
	public String activeSanctuaryLook;
	public boolean sanctuaryPreloadCompleted = false;

	public HashMap<String, Long> respawnItems = new HashMap<String, Long>();

	public int pendingLookDefID = 8254;
	public String pendingLookID = null;

	public boolean roomReady = false;
	public boolean wasInChat = false;

	public int levelType = 0;
	public int previousLevelType = 0;
	public int levelID = 0;
	public int pendingLevelID = 0;
	public int previousLevelID = 0;

	public String pendingRoom = "0";
	public String room = null;

	public String respawn = null;
	public String lastLocation = null;

	// TODO: Clean up into vector3 type.
	public double lastPosX = 0;
	public double lastPosY = -1000;
	public double lastPosZ = 0;

	// TODO: Clean up into quaternion type.
	public double lastRotW = 0;
	public double lastRotX = 0;
	public double lastRotY = 0;
	public double lastRotZ = 0;

	public ActorActionType lastAction;

	// Teleports
	public String teleportDestination;
	public Vector3 targetPos;
	public Quaternion targetRot;
	
	// Trades
	public Trade tradeEngagedIn;
	
	public void destroyAt(Player player) {
		// Delete character
		WorldObjectDelete packet = new WorldObjectDelete(account.getAccountID());
		player.client.sendPacket(packet);
		lastAction = ActorActionType.None;
	}

	public void syncTo(Player player) {
		// Find avatar
		JsonArray items = account.getPlayerInventory().getItem("avatars").getAsJsonArray();
		JsonObject lookObj = null;
		for (JsonElement itm : items) {
			if (itm.isJsonObject()) {
				JsonObject obj = itm.getAsJsonObject();
				if (obj.get("id").getAsString().equals(activeLook)) {
					lookObj = obj;
					break;
				}
			}
		}

		if (lookObj != null) {

			// Spawn player
			PlayerWorldObjectInfo packet = new PlayerWorldObjectInfo();

			// Object creation parameters
			packet.id = account.getAccountID();
			packet.defId = 852; // TODO: Move to static final (const)
			packet.ownerId = account.getAccountID();

			packet.lastMove = new WorldObjectMoveNodeData();
			packet.lastMove.actorActionType = ActorActionType.Respawn; // TODO: Is this the right actor action type for
																		// a player that's spawning in?
			packet.lastMove.serverTime = System.currentTimeMillis() / 1000;
			packet.lastMove.positionInfo = new WorldObjectPositionInfo(lastPosX, lastPosY, lastPosZ, lastRotX, lastRotY,
					lastRotZ, lastRotW);
			packet.lastMove.velocity = new Velocity();
			packet.lastMove.nodeType = WorldObjectMoverNodeType.InitPosition;

			// Look and name
			packet.look = lookObj.get("components").getAsJsonObject().get("AvatarLook").getAsJsonObject().get("info")
					.getAsJsonObject();
			packet.displayName = account.getDisplayName();
			packet.unknownValue = 0; // TODO: What is this??

			player.client.sendPacket(packet);
		}
	}

	/**
	 * Attempts to teleport the player to another player's sanctuary.
	 * 
	 * @param sanctuaryOwner The owner of the sanctuary.
	 * @return If the join was a success or not.
	 */
	public boolean teleportToSanctuary(String sanctuaryOwner) {
		try {
			boolean isAllowed = true;

			// Load player object
			Player player = this;

			// Find owner
			CenturiaAccount sancOwner = AccountManager.getInstance().getAccount(sanctuaryOwner);
			if (!sancOwner.getPlayerInventory().containsItem("201")) {
				Player plr = sancOwner.getOnlinePlayerInstance();
				if (plr != null)
					plr.activeSanctuaryLook = sancOwner.getActiveSanctuaryLook();
			}

			// Check owner
			boolean isOwner = player.account.getAccountID().equals(sanctuaryOwner);

			if (!isOwner) {
				// Load privacy settings
				int privSetting = 0;
				UserVarValue val = sancOwner.getPlayerInventory().getUserVarAccesor().getPlayerVarValue(17544, 0);
				if (val != null)
					privSetting = val.value;

				// Verify access
				if (privSetting == 2) {
					// Nobody
					isAllowed = false;
				} else if (privSetting == 1) {
					// Followers
					// Check if the owner follows the current player
					if (!SocialManager.getInstance().getPlayerIsFollowing(sanctuaryOwner,
							player.account.getAccountID())) {
						isAllowed = false;
					}
				}
			}

			// Build room join
			JoinRoom join = new JoinRoom();
			join.success = isAllowed;
			join.levelType = 2;
			join.levelID = 1689;
			join.roomIdentifier = "sanctuary_" + sanctuaryOwner;
			join.teleport = sanctuaryOwner;

			if (isAllowed == true) {
				// Sync
				GameServer srv = (GameServer) client.getServer();
				for (Player plr2 : srv.getPlayers()) {
					if (plr2.room != null && player.room != null && player.room != null && plr2.room.equals(player.room)
							&& plr2 != player) {
						player.destroyAt(plr2);
					}
				}

				// Assign room
				player.roomReady = false;
				player.pendingLevelID = 1689;
				player.pendingRoom = "sanctuary_" + sanctuaryOwner;
				player.levelType = join.levelType;
			} else {
				client.sendPacket(join);
				return false;
			}

			// Send packet
			client.sendPacket(join);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			client.sendPacket(new JoinRoom().markAsFailed());
			return false;
		}
	}

	/**
	 * Attempts to teleport the player to another player's sanctuary, targeting a
	 * player to teleport to.
	 * 
	 * @param sanctuaryOwner The owner of the sanctuary.
	 * @param targetedPlayer the player targeted to teleport to.
	 * @return If the join was a success or not.
	 */
	private boolean teleportToSanctuary(String sanctuaryOwner, Player targetedPlayer) {
		try {
			boolean isAllowed = true;

			// Load player object
			Player player = this;

			// Find owner
			CenturiaAccount sancOwner = AccountManager.getInstance().getAccount(sanctuaryOwner);
			if (!sancOwner.getPlayerInventory().containsItem("201")) {
				Player plr = sancOwner.getOnlinePlayerInstance();
				if (plr != null)
					plr.activeSanctuaryLook = sancOwner.getActiveSanctuaryLook();
			}

			// Check owner
			boolean isOwner = player.account.getAccountID().equals(sanctuaryOwner);

			if (!isOwner) {
				// Load privacy settings
				int privSetting = 0;
				UserVarValue val = sancOwner.getPlayerInventory().getUserVarAccesor().getPlayerVarValue(17544, 0);
				if (val != null)
					privSetting = val.value;

				// Verify access
				if (privSetting == 2) {
					// Nobody
					isAllowed = false;
				} else if (privSetting == 1) {
					// Followers
					// Check if the owner follows the current player
					if (!SocialManager.getInstance().getPlayerIsFollowing(sanctuaryOwner,
							player.account.getAccountID())) {
						isAllowed = false;
					}
				}
			}

			// Build room join
			JoinRoom join = new JoinRoom();
			join.success = isAllowed;
			join.levelType = 2;
			join.levelID = 1689;
			join.roomIdentifier = "sanctuary_" + sanctuaryOwner;
			join.teleport = sanctuaryOwner;

			if (isAllowed == true) {
				// Sync
				GameServer srv = (GameServer) client.getServer();
				for (Player plr2 : srv.getPlayers()) {
					if (plr2.room != null && player.room != null && player.room != null && plr2.room.equals(player.room)
							&& plr2 != player) {
						player.destroyAt(plr2);
					}
				}

				// Assign room
				player.roomReady = false;
				player.pendingLevelID = 1689;
				player.pendingRoom = "sanctuary_" + sanctuaryOwner;
				player.levelType = join.levelType;
				player.teleportDestination = targetedPlayer.account.getAccountID();
				player.targetPos = new Vector3(targetedPlayer.lastPosX, targetedPlayer.lastPosY,
						targetedPlayer.lastPosZ);
				player.targetRot = new Quaternion(targetedPlayer.lastRotX, targetedPlayer.lastRotY,
						targetedPlayer.lastRotZ, targetedPlayer.lastRotW);
			} else {
				client.sendPacket(join);
				return false;
			}

			// Send packet
			client.sendPacket(join);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			client.sendPacket(new JoinRoom().markAsFailed());
			return false;
		}

	}

	/**
	 * Attempts to teleport the player to a specific room.
	 * 
	 * @param levelID        The id of the level.
	 * @param levelType      The type of the level.
	 * @param iisRoomID      The IIS ROOM ID (UNUSED CURRENTLY)
	 * @param roomIdentifier The string indentifier for the level.
	 * @param teleport       TODO: I'm not sure what this is?
	 * @return If the join was a success or not.
	 */
	public boolean teleportToRoom(int levelID, int levelType, int iisRoomID, String roomIdentifier, String teleport) {
		try {
			// Load the requested room
			Player plr = this;

			// Sync
			GameServer srv = (GameServer) client.getServer();
			for (Player player : srv.getPlayers()) {
				if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
					plr.destroyAt(player);
				}
			}

			// Assign room
			JoinRoom join = new JoinRoom();
			join.levelType = levelType;
			join.levelID = levelID;
			join.teleport = teleport;
			join.roomIdentifier = roomIdentifier;

			plr.roomReady = false;
			plr.pendingLevelID = levelID;
			plr.pendingRoom = "room_" + levelID;
			plr.levelType = levelType;

			plr.respawnItems.clear();

			// Log
			if (System.getProperty("debugMode") != null) {
				System.out.println("[JOINROOM] Client to server (room: " + plr.pendingRoom + ", level: "
						+ plr.pendingLevelID + ")");
			}

			// Send response
			client.sendPacket(join);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			client.sendPacket(new JoinRoom().markAsFailed());
			return false;
		}
	}

	/**
	 * Attempts to teleport the player to a specific room, targeting a player to
	 * teleport to.
	 * 
	 * @param levelID        The id of the level.
	 * @param levelType      The type of the level.
	 * @param iisRoomID      The IIS ROOM ID (UNUSED CURRENTLY)
	 * @param roomIdentifier The string indentifier for the level.
	 * @param teleport       TODO: I'm not sure what this is?
	 * @param targetedPlayer the player targeted to teleport to.
	 * @return If the join was a success or not.
	 */
	private boolean teleportToRoom(int levelID, int levelType, int iisRoomID, String roomIdentifier, String teleport,
			Player targetedPlayer) {
		try {
			// Load the requested room
			Player plr = this;

			// Sync
			GameServer srv = (GameServer) client.getServer();
			for (Player player : srv.getPlayers()) {
				if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
					plr.destroyAt(player);
				}
			}

			// Assign room
			JoinRoom join = new JoinRoom();
			join.levelType = levelType;
			join.levelID = levelID;
			join.roomIdentifier = plr.pendingRoom;
			join.teleport = teleport;
			join.roomIdentifier = roomIdentifier;

			plr.roomReady = false;
			plr.pendingLevelID = levelID;
			plr.pendingRoom = "room_" + levelID;
			plr.levelType = levelType;

			plr.teleportDestination = targetedPlayer.account.getAccountID();
			plr.targetPos = new Vector3(targetedPlayer.lastPosX, targetedPlayer.lastPosY, targetedPlayer.lastPosZ);
			plr.targetRot = new Quaternion(targetedPlayer.lastRotX, targetedPlayer.lastRotY, targetedPlayer.lastRotZ,
					targetedPlayer.lastRotW);

			plr.respawnItems.clear();

			// Log
			if (System.getProperty("debugMode") != null) {
				System.out.println("[JOINROOM] Client to server (room: " + plr.pendingRoom + ", level: "
						+ plr.pendingLevelID + ")");
			}

			// Send response
			client.sendPacket(join);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			client.sendPacket(new JoinRoom().markAsFailed());
			return false;
		}
	}

	/**
	 * Attempts to teleport to the previous room.
	 * 
	 * @return If the join was a success or not.
	 */
	public boolean teleportToPreviousRoom() {
		try {
			var plr = this;

			// Assign room
			plr.roomReady = false;
			plr.pendingLevelID = plr.previousLevelID;
			plr.pendingRoom = "room_" + plr.previousLevelID;
			plr.levelType = plr.previousLevelType;

			// Send response
			JoinRoom join = new JoinRoom();
			join.levelType = plr.levelType;
			join.levelID = plr.pendingLevelID;
			client.sendPacket(join);

			// Log
			if (System.getProperty("debugMode") != null) {
				System.out.println("[JOINROOM]  Client to server (room: " + plr.pendingRoom + ", level: "
						+ plr.pendingLevelID + ")");
			}

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	/**
	 * Attempts to teleport to the specified player.
	 * 
	 * @param targetPlayerAccountId the player id to teleport to.
	 * @return If the join was a success or not.
	 */
	public boolean teleportToPlayer(String targetPlayerAccountId) {
		try {
			Player player = this;

			// Find player
			for (Player plr : ((GameServer) client.getServer()).getPlayers()) {
				if (plr.account.getAccountID().equals(targetPlayerAccountId) && plr.roomReady
						&& !plr.room.equals("room_STAFFROOM")
						&& (!SocialManager.getInstance().socialListExists(targetPlayerAccountId)
								|| !SocialManager.getInstance().getPlayerIsBlocked(targetPlayerAccountId,
										player.account.getAccountID()))) {
					// Load privacy settings
					int privSetting = 0;
					UserVarValue val = plr.account.getPlayerInventory().getUserVarAccesor().getPlayerVarValue(17546, 0);
					if (val != null)
						privSetting = val.value;

					// Verify privacy settings
					if (privSetting == 1 && !SocialManager.getInstance()
							.getPlayerIsFollowing(plr.account.getAccountID(), player.account.getAccountID()))
						break;
					else if (privSetting == 2)
						break;

					boolean success = true;
					if (!plr.room.equals(player.room)) {
						// Build room join

						if (plr.room.contains("sanctuary")) {
							// how can I get the sanctuary owner...
							String ownerId = plr.room.substring(plr.room.indexOf('_') + 1);
							success = player.teleportToSanctuary(ownerId, plr);
						} else {
							success = player.teleportToRoom(plr.levelID, plr.levelType, -1, "room_" + plr.levelID, "",
									plr);
						}

						var jumpToPlayerResponse = new JumpToPlayer();
						jumpToPlayerResponse.success = success;
						client.sendPacket(jumpToPlayerResponse);
						return true;
					} else {
						// TODO: This is weird.
						XtWriter writer = new XtWriter();
						writer.writeString("rfjtr");
						writer.writeInt(-1); // data prefix
						writer.writeInt(1); // other world
						writer.writeString("");
						writer.writeString(""); // data suffix
						client.sendPacket(writer.encode());
						return true;
					}
				}
			}

			var jumpToPlayerResponse = new JumpToPlayer();
			jumpToPlayerResponse.success = false;
			client.sendPacket(jumpToPlayerResponse);
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			var jumpToPlayerResponse = new JumpToPlayer();
			jumpToPlayerResponse.success = false;
			client.sendPacket(jumpToPlayerResponse);
			return false;
		}
	}
	
	/**
	 * Gets the players trade list. Will load the trade list if it hasn't been loaded before. 
	 * @return A list of items in the player's trade list.
	 */
	public List<InventoryItem> getTradeList()
	{
		try {
			return this.account.getPlayerInventory().getItemAccessor(this).loadTradeList();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}
	

}
