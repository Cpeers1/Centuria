package org.asf.centuria.networking.chatserver.networking;

import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.rooms.ChatRoom;
import org.asf.centuria.networking.chatserver.rooms.ChatRoomTypes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class UserConversations extends AbstractChatPacket {

	@Override
	public String id() {
		return "users.conversations";
	}

	@Override
	public AbstractChatPacket instantiate() {
		return new UserConversations();
	}

	@Override
	public void parse(JsonObject data) {
	}

	@Override
	public void build(JsonObject data) {
	}

	@Override
	public boolean handle(ChatClient client) {
		// Send response
		JsonObject res = new JsonObject();
		JsonArray convos = new JsonArray();

		// Add room objects
		for (ChatRoom room : client.getRoomInstances()) {
			if (room.getType().equalsIgnoreCase(ChatRoomTypes.PRIVATE_CHAT)) {
				JsonObject obj = client.getServer().roomObject(room.getRoomID(), room.getType(),
						client.getPlayer().getAccountID());
				if (obj != null)
					convos.add(obj);
			}
		}
		res.add("conversations", convos);
		res.addProperty("eventId", "users.conversations");
		res.addProperty("success", true);
		client.sendPacket(res);

		return true;
	}

}
