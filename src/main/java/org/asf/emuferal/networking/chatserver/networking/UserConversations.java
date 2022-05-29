package org.asf.emuferal.networking.chatserver.networking;

import org.asf.emuferal.networking.chatserver.ChatClient;

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
		for (String room : client.getRooms()) {
			if (client.isRoomPrivate(room))
				convos.add(client.getServer().roomObject(room, true));
		}

		res.add("conversations", convos);
		res.addProperty("eventId", "users.conversations");
		res.addProperty("success", true);
		client.sendPacket(res);

		return true;
	}

}
