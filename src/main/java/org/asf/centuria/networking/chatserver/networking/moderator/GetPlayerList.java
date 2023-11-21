package org.asf.centuria.networking.chatserver.networking.moderator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.asf.centuria.Centuria;
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
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader().getResourceAsStream("spawns.json");
			helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject().get("Maps")
					.getAsJsonObject();
			strm.close();
		} catch (Exception e) {
		}

		// Locate suspicious clients
		HashMap<ChatClient, String> suspiciousClients = new HashMap<ChatClient, String>();
		for (ChatClient cl : client.getServer().getClients()) {
			Player plr = cl.getPlayer().getOnlinePlayerInstance();
			if (plr == null) {
				suspiciousClients.put(cl, "no_gameserver_connection");
			} else if ((!plr.roomReady || plr.room == null) && plr.levelID != 25280) {
				suspiciousClients.put(cl, "limbo");
			}
		}

		// Find level IDs
		int ingame = 0;
		ArrayList<Integer> levelIDs = new ArrayList<Integer>();
		HashMap<Integer, ArrayList<String>> rooms = new HashMap<Integer, ArrayList<String>>();
		for (ChatClient cl : client.getServer().getClients()) {
			Player plr = cl.getPlayer().getOnlinePlayerInstance();
			if (plr != null && !suspiciousClients.containsKey(cl)) {
				// Increase count
				ingame++;

				// Add
				if (!levelIDs.contains(plr.levelID)) {
					levelIDs.add(plr.levelID);
					rooms.put(plr.levelID, new ArrayList<String>());
				}
				ArrayList<String> rLst = rooms.get(plr.levelID);

				// Find room instances
				GameRoom room = plr.getRoom();
				if (room != null && room.getLevelID() == plr.levelID && !rLst.contains(room.getInstanceID())) {
					rLst.add(room.getInstanceID());
				}
			}
		}

		// Build message
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
		for (int levelID : levelIDs) {
			// Create level object
			JsonObject level = new JsonObject();
			levels.add(Integer.toString(levelID), level);
			JsonObject roomsL = new JsonObject();
			String map = "UNKOWN: " + levelID;
			if (levelID == 25280)
				map = "Tutorial";
			else if (helper.has(Integer.toString(levelID)))
				map = helper.get(Integer.toString(levelID)).getAsString();
			level.addProperty("levelID", levelID);
			level.addProperty("levelName", map);
			level.add("rooms", roomsL);

			// Find rooms
			for (String roomID : rooms.get(levelID)) {
				// Create room
				JsonObject roomObj = new JsonObject();
				roomsL.add(roomID, roomObj);
				for (ChatClient cl : client.getServer().getClients()) {
					// Get player
					Player plr = cl.getPlayer().getOnlinePlayerInstance();
					if (plr != null && plr.room != null && !suspiciousClients.containsKey(cl)) {
						// Add to response
						if (!clientsR.has(plr.room))
							clientsR.add(plr.room, new JsonObject());
						JsonObject rObj = clientsR.get(plr.room).getAsJsonObject();
						rObj.addProperty(cl.getPlayer().getAccountID(), cl.getPlayer().getDisplayName());

						// Check
						GameRoom room = plr.getRoom();
						if (room != null && room.getLevelID() == levelID && room.getInstanceID().equals(roomID)) {
							// Add to response
							roomObj.addProperty(cl.getPlayer().getAccountID(), cl.getPlayer().getDisplayName());
						}
					} else if (!suspiciousClients.containsKey(cl)) {
						suspiciousClients.put(cl, "no_gameserver_connection");
					}
				}
			}

			// Default players
			for (ChatClient cl : client.getServer().getClients()) {
				// Get player
				Player plr = cl.getPlayer().getOnlinePlayerInstance();
				if (plr != null && !suspiciousClients.containsKey(cl)) {
					// Check
					GameRoom room = plr.getRoom();
					if (room == null && plr.levelID == levelID && plr.room != null) {
						// Add to response
						if (!clientsR.has(plr.room))
							clientsR.add(plr.room, new JsonObject());
						JsonObject rObj = clientsR.get(plr.room).getAsJsonObject();
						rObj.addProperty(cl.getPlayer().getAccountID(), cl.getPlayer().getDisplayName());
					}
				} else if (!suspiciousClients.containsKey(cl)) {
					suspiciousClients.put(cl, "no_gameserver_connection");
				}
			}
		}

		// Add suspicious clients
		if (suspiciousClients.size() != 0) {
			for (ChatClient cl : suspiciousClients.keySet()) {
				// Check moderator perms
				String permLevel2 = "member";
				if (cl.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
					permLevel2 = cl.getPlayer().getSaveSharedInventory().getItem("permissions").getAsJsonObject()
							.get("permissionLevel").getAsString();
				}
				if (GameServer.hasPerm(permLevel2, "moderator"))
					continue;

				// Add
				JsonObject clD = new JsonObject();
				clD.addProperty("id", cl.getPlayer().getAccountID());
				clD.addProperty("name", cl.getPlayer().getDisplayName());
				clD.addProperty("reason", suspiciousClients.get(cl));
				susClients.add(cl.getPlayer().getAccountID(), clD);
			}
		}

		// Send response
		client.sendPacket(response);
		return true;
	}

}
