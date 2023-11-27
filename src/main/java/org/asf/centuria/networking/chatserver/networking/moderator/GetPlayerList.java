package org.asf.centuria.networking.chatserver.networking.moderator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.networking.AbstractChatPacket;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.centuria.rooms.GameRoom;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GetPlayerList extends AbstractChatPacket {

	@Override
	public String id() {
		return "centuria.moderatorclient.getplayers";
	}

	@Override
	public AbstractChatPacket instantiate() {
		return new GetPlayerList();
	}

	@Override
	public void parse(JsonObject data) {
	}

	@Override
	public void build(JsonObject data) {
	}

	@Override
	public boolean handle(ChatClient client) {
		// Check moderator client
		if (client.getObject(ModeratorClient.class) == null)
			return true;

		// Check moderator perms
		String permLevel = "member";
		if (client.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
			permLevel = client.getPlayer().getSaveSharedInventory().getItem("permissions").getAsJsonObject()
					.get("permissionLevel").getAsString();
		}
		if (!GameServer.hasPerm(permLevel, "moderator"))
			return true;

		// Create list
		// Load spawn helper
		JsonObject helper = null;
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("content/world/spawns.json");
			helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject().get("Maps")
					.getAsJsonObject();
			strm.close();
		} catch (Exception e) {
		}

		// Locate suspicious clients from chat server
		ArrayList<String> mapLessClients = new ArrayList<String>();
		HashMap<CenturiaAccount, String> suspiciousClients = new HashMap<CenturiaAccount, String>();
		for (ChatClient cl : client.getServer().getClients()) {
			if (!mapLessClients.contains(cl.getPlayer().getAccountID())) {
				Player plr = cl.getPlayer().getOnlinePlayerInstance();
				if (plr == null) {
					// Check perms
					String permLevel2 = "member";
					if (cl.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
						permLevel2 = cl.getPlayer().getSaveSharedInventory().getItem("permissions").getAsJsonObject()
								.get("permissionLevel").getAsString();
					}
					if (GameServer.hasPerm(permLevel2, "moderator"))
						continue;

					// No game server
					mapLessClients.add(cl.getPlayer().getAccountID());
					suspiciousClients.put(cl.getPlayer(), "no_gameserver_connection");
				} else if ((!plr.roomReady || plr.room == null) && plr.levelID != 25280) {
					// In limbo
					mapLessClients.add(cl.getPlayer().getAccountID());
					suspiciousClients.put(cl.getPlayer(), "limbo");
				}
			}
		}

		// Find level IDs
		int ingame = 0;
		ArrayList<String> playerIDs = new ArrayList<String>();
		ArrayList<Integer> levelIDs = new ArrayList<Integer>();
		HashMap<Integer, ArrayList<String>> rooms = new HashMap<Integer, ArrayList<String>>();
		HashMap<Player, String> playersInRooms = new HashMap<Player, String>();
		for (Player plr : Centuria.gameServer.getPlayers()) {
			if (!playerIDs.contains(plr.account.getAccountID()) && !mapLessClients.contains(plr.account.getAccountID())
					&& (plr.roomReady || plr.levelID == 25280)) {
				// Increase count
				playerIDs.add(plr.account.getAccountID());
				ingame++;

				// Add level if missing
				if (!levelIDs.contains(plr.levelID)) {
					levelIDs.add(plr.levelID);
					rooms.put(plr.levelID, new ArrayList<String>());
				}

				// Add to room map
				if (plr.room != null)
					playersInRooms.put(plr, plr.room);

				// Get room list
				ArrayList<String> rLst = rooms.get(plr.levelID);

				// Find room instances
				GameRoom room = plr.getRoom();
				if (room != null && room.getLevelID() == plr.levelID && !rLst.contains(room.getInstanceID())) {
					rLst.add(room.getInstanceID());
				}
			}
		}

		// Build response object
		JsonObject response = new JsonObject();
		response.addProperty("eventId", "centuria.moderatorclient.playerlist");
		response.addProperty("connected", Centuria.gameServer.getPlayers().length);
		response.addProperty("ingame", ingame);
		JsonObject levels = new JsonObject();
		response.add("playersByLevel", levels);
		JsonObject clientsR = new JsonObject();
		response.add("playersByRoom", clientsR);
		JsonObject susClients = new JsonObject();
		response.add("suspiciousClients", susClients);

		// Add each level
		playerIDs = new ArrayList<String>();
		for (int levelID : levelIDs) {
			// Determine map name
			String map = "UNKOWN: " + levelID;
			if (levelID == 25280)
				map = "Tutorial";
			else if (helper.has(Integer.toString(levelID)))
				map = helper.get(Integer.toString(levelID)).getAsString();

			// Create level object
			JsonObject level = new JsonObject();
			levels.add(Integer.toString(levelID), level);
			JsonObject roomsL = new JsonObject();
			level.addProperty("levelID", levelID);
			level.addProperty("levelName", map);
			level.add("rooms", roomsL);

			// Find rooms
			for (String roomID : rooms.get(levelID)) {
				// Create room
				JsonObject roomObj = new JsonObject();
				roomsL.add(roomID, roomObj);

				// Find players in rooms
				for (Player plr : playersInRooms.keySet()) {
					String plrRoom = playersInRooms.get(plr);
					if (!playerIDs.contains(plr.account.getAccountID())) {
						// Make sure it doesnt get added more than once
						playerIDs.add(plr.account.getAccountID());

						// Add to response
						if (!clientsR.has(plrRoom))
							clientsR.add(plrRoom, new JsonObject());
						JsonObject rObj = clientsR.get(plrRoom).getAsJsonObject();
						rObj.addProperty(plr.account.getAccountID(), plr.account.getDisplayName());

						// Check
						GameRoom room = plr.getRoom();
						if (room != null && room.getLevelID() == levelID && room.getInstanceID().equals(roomID)) {
							// Add to response
							roomObj.addProperty(plr.account.getAccountID(), plr.account.getDisplayName());
						}
					}
				}
			}

			// Players in other rooms
			for (Player plr : playersInRooms.keySet()) {
				String plrRoom = playersInRooms.get(plr);
				if (!mapLessClients.contains(plr.account.getAccountID())
						&& !playerIDs.contains(plr.account.getAccountID())) {
					// Check
					GameRoom room = ((GameServer) plr.client.getServer()).getRoomManager().getRoom(plrRoom);
					if (room == null && plr.levelID == levelID) {
						// Add to response
						if (!clientsR.has(plrRoom))
							clientsR.add(plrRoom, new JsonObject());
						JsonObject rObj = clientsR.get(plrRoom).getAsJsonObject();
						rObj.addProperty(plr.account.getAccountID(), plr.account.getDisplayName());
					}
				}
			}
		}

		// Add suspicious clients
		if (suspiciousClients.size() != 0) {
			for (CenturiaAccount acc : suspiciousClients.keySet()) {
				// Check moderator perms
				String permLevel2 = "member";
				if (acc.getSaveSharedInventory().containsItem("permissions")) {
					permLevel2 = acc.getSaveSharedInventory().getItem("permissions").getAsJsonObject()
							.get("permissionLevel").getAsString();
				}
				if (GameServer.hasPerm(permLevel2, "moderator"))
					continue;

				// Add
				JsonObject clD = new JsonObject();
				clD.addProperty("id", acc.getAccountID());
				clD.addProperty("name", acc.getDisplayName());
				clD.addProperty("reason", suspiciousClients.get(acc));
				susClients.add(acc.getAccountID(), clD);
			}
		}

		// Send response
		client.sendPacket(response);
		return true;
	}

}
