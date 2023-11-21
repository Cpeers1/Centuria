package org.asf.centuria.networking.chatserver.networking;

import java.util.UUID;

import org.asf.centuria.dms.DMManager;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.rooms.ChatRoomTypes;
import org.asf.centuria.networking.gameserver.GameServer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class GetConversation extends AbstractChatPacket {

	private String convo;
	private static String NIL_UUID = new UUID(0, 0).toString();

	@Override
	public String id() {
		return "conversations.get";
	}

	@Override
	public AbstractChatPacket instantiate() {
		return new GetConversation();
	}

	@Override
	public void parse(JsonObject data) {
		convo = data.get("conversationId").getAsString();
	}

	@Override
	public void build(JsonObject data) {
	}

	@Override
	public boolean handle(ChatClient client) {
		// System messages
		if (convo.equals(NIL_UUID)) {
			// Build object
			JsonObject roomData = new JsonObject();
			roomData.addProperty("conversation_id", NIL_UUID);
			roomData.addProperty("title", "Centuria Server messages");

			// Build participants object
			JsonArray members = new JsonArray();
			members.add(NIL_UUID);
			members.add(client.getPlayer().getAccountID());
			roomData.add("participants", members);
			roomData.addProperty("conversationType", "private");

			// Send packet
			JsonObject res = new JsonObject();
			res.add("conversation", roomData);
			res.addProperty("eventId", "conversations.get");
			res.addProperty("success", true);
			client.sendPacket(res);
			return true;
		}

		// Check
		if (client.getRoom(convo) == null) {
			// Find other player in this room first
			boolean found = false;
			for (ChatClient cl : client.getServer().getClients()) {
				if (cl.isInRoom(convo)) {
					found = true;
					client.joinRoom(convo, cl.getRoom(convo).getType());
					break;
				}
			}

			// Find by room
			if (!found) {
				// Check sanctuary
				if (convo.startsWith("sanctuary_")) {
					// Sanctuary
					client.joinRoom(convo, ChatRoomTypes.ROOM_CHAT);
					found = true;
				} else {
					// Find room in room manager
					Player plr = client.getPlayer().getOnlinePlayerInstance();
					if (plr != null) {
						GameServer server = (GameServer) plr.client.getServer();
						if (server.getRoomManager().getRoom(convo) != null) {
							// Found room chat
							client.joinRoom(convo, ChatRoomTypes.ROOM_CHAT);
							found = true;
						}
					}

					// Check
					if (!found) {
						// DMs
						if (DMManager.getInstance().dmExists(convo)) {
							// Found DM chat
							client.joinRoom(convo, ChatRoomTypes.PRIVATE_CHAT);
							found = true;
						} else {
							// Transient
							client.joinRoom(convo, ChatRoomTypes.TRANSIENT_CHAT);
							found = true;
						}
					}
				}
			}
		}

		// Send response
		JsonObject res = new JsonObject();
		res.add("conversation", client.getServer().roomObject(convo, client.getRoom(convo).getType(),
				client.getPlayer().getAccountID()));
		res.addProperty("eventId", "conversations.get");
		res.addProperty("success", true);
		client.sendPacket(res);

		return true;
	}

}
