package org.asf.centuria.networking.chatserver.networking.moderator;

import java.io.InputStream;
import java.util.ArrayList;

import org.asf.centuria.Centuria;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.networking.AbstractChatPacket;
import org.asf.centuria.networking.chatserver.rooms.ChatRoomTypes;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.centuria.rooms.GameRoom;

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

		// Prepare response
		JsonObject response = new JsonObject();
		response.addProperty("eventId", "centuria.moderatorclient.chatrooms");
		response.addProperty("connected", Centuria.gameServer.getPlayers().length);
		JsonObject rooms = new JsonObject();
		response.add("rooms", rooms);
		JsonArray activeRooms = new JsonArray();
		response.add("active", activeRooms);
		ArrayList<String> activeRoomList = new ArrayList<String>();

		// Get rooms of all players
		for (ChatClient cl : client.getServer().getClients()) {
			for (String room : cl.getRooms()) {
				if (!cl.getRoom(room).getType().equals(ChatRoomTypes.PRIVATE_CHAT)) {
					// Add if not present
					if (!rooms.has(room)) {
						// Check name
						if (room.startsWith("sanctuary_")) {
							// Add sanctuary room
							JsonObject roomObj = new JsonObject();
							roomObj.addProperty("roomType", "sanctuary");
							roomObj.addProperty("roomLevelID", 1689);
							roomObj.addProperty("roomLevelName", "Sanctuary");
							roomObj.addProperty("roomInstancePresent", false);
							roomObj.addProperty("sanctuaryOwner", room.substring("sanctuary_".length()));
							rooms.add(room, roomObj);
						} else {
							// Add regular room
							JsonObject roomObj = new JsonObject();
							roomObj.addProperty("roomType", cl.getRoom(room).getType());
							// Find special properties
							GameRoom roomInst = Centuria.gameServer.getRoomManager().getRoom(room);
							if (roomInst != null) {
								// Find map
								String map = "UNKOWN: " + roomInst.getLevelID();
								if (roomInst.getLevelID() == 25280)
									map = "Tutorial";
								else if (helper.has(Integer.toString(roomInst.getLevelID())))
									map = helper.get(Integer.toString(roomInst.getLevelID())).getAsString();
								roomObj.addProperty("roomLevelID", roomInst.getLevelID());
								roomObj.addProperty("roomLevelName", map);
								roomObj.addProperty("roomInstancePresent", true);
								roomObj.addProperty("roomInstanceID", roomInst.getInstanceID());
							}
							rooms.add(room, roomObj);
						}

						// Add to active rooms
						if (!activeRoomList.contains(room)) {
							activeRoomList.add(room);
							activeRooms.add(room);
						}
					}
				}
			}
		}

		// Find other game rooms
		for (GameRoom room : Centuria.gameServer.getRoomManager().getAllRooms()) {
			// Add game room
			if (!rooms.has(room.getID()))
				rooms.add(room.getID(), new JsonObject());
			JsonObject roomObj = rooms.get(room.getID()).getAsJsonObject();
			roomObj.addProperty("roomType", ChatRoomTypes.ROOM_CHAT);

			// Find map
			String map = "UNKOWN: " + room.getLevelID();
			if (room.getLevelID() == 25280)
				map = "Tutorial";
			else if (helper.has(Integer.toString(room.getLevelID())))
				map = helper.get(Integer.toString(room.getLevelID())).getAsString();
			roomObj.addProperty("roomLevelID", room.getLevelID());
			roomObj.addProperty("roomLevelName", map);
			roomObj.addProperty("roomInstancePresent", true);
			roomObj.addProperty("roomInstanceID", room.getInstanceID());
		}

		// Send response
		client.sendPacket(response);
		return true;
	}

}
