package org.asf.centuria.networking.chatserver.networking.moderator;

import java.io.InputStream;
import java.util.ArrayList;

import org.asf.centuria.Centuria;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.networking.AbstractChatPacket;
import org.asf.centuria.networking.chatserver.rooms.ChatRoom;
import org.asf.centuria.networking.chatserver.rooms.ChatRoomTypes;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GetChatRoomList extends AbstractChatPacket {

	@Override
	public String id() {
		return "centuria.moderatorclient.getchatrooms";
	}

	@Override
	public AbstractChatPacket instantiate() {
		return new GetChatRoomList();
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

		// Prepare response
		JsonObject response = new JsonObject();
		response.addProperty("eventId", "centuria.moderatorclient.chatrooms");
		response.addProperty("connected", Centuria.gameServer.getPlayers().length);
		JsonObject rooms = new JsonObject();
		response.add("rooms", rooms);
		JsonArray activeRooms = new JsonArray();
		response.add("active", activeRooms);
		ArrayList<String> activeRoomList = new ArrayList<String>();

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

		// Get rooms of all players
		for (ChatClient cl : client.getServer().getClients()) {
			for (ChatRoom room : cl.getRoomInstances()) {
				if (!room.getType().equalsIgnoreCase(ChatRoomTypes.PRIVATE_CHAT)) {
					// Add if not present
					if (!rooms.has(room.getRoomID())) {
						// Check name
						if (room.getRoomID().startsWith("sanctuary_")) {
							// Add sanctuary room
							JsonObject roomObj = new JsonObject();
							roomObj.addProperty("roomType", "sanctuary");
							roomObj.addProperty("roomLevelID", "1689");
							roomObj.addProperty("roomLevelName", "Sanctuary");
							roomObj.addProperty("roomInstancePresent", false);
							roomObj.addProperty("sanctuaryOwner", room.getRoomID().substring("sanctuary_".length()));
							rooms.add(room.getRoomID(), roomObj);
						} else {
							// Add regular room
							JsonObject roomObj = new JsonObject();
							roomObj.addProperty("roomType", room.getType());

							// Find map
							if (room.getRoomID().startsWith("room_")) {
								String levelId = room.getRoomID().substring("room_".length()); // FIXME: wont work
																								// forever, eventually
																								// room instancing will
																								// be implemented and
																								// this will break
								String map = "UNKNOWN: " + levelId;
								if (levelId.equals("25280"))
									map = "Tutorial";
								else if (helper.has(levelId))
									map = helper.get(levelId).getAsString();
								roomObj.addProperty("roomLevelID", levelId);
								roomObj.addProperty("roomLevelName", map);
							}
							roomObj.addProperty("roomInstancePresent", true);
							rooms.add(room.getRoomID(), roomObj);
						}

						// Add to active rooms
						if (!activeRoomList.contains(room.getRoomID())) {
							activeRoomList.add(room.getRoomID());
							activeRooms.add(room.getRoomID());
						}
					}
				}
			}
		}

		// Send response
		client.sendPacket(response);
		return true;
	}

}
