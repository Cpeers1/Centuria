package org.asf.centuria.networking.chatserver.networking.moderator;

import java.util.ArrayList;

import org.asf.centuria.Centuria;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.networking.AbstractChatPacket;
import org.asf.centuria.networking.gameserver.GameServer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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

		// Get rooms of all players
		for (ChatClient cl : client.getServer().getClients()) {
			for (String room : cl.getRooms()) {
				if (!cl.isRoomPrivate(room)) {
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
							roomObj.addProperty("roomType", cl.isRoomPrivate(room) ? "private" : "room");
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

		// Send response
		client.sendPacket(response);
		return true;
	}

}
