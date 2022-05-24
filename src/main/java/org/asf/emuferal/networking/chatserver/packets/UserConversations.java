package org.asf.emuferal.networking.chatserver.packets;

import java.util.HashMap;

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

		// Send response (we are not going to use this too much)
		JsonObject res = new JsonObject();
		JsonArray convos = new JsonArray();

		// Find rooms
		HashMap<String, Boolean> conversations = new HashMap<String, Boolean>();
		for (ChatClient cl : client.getServer().getClients()) {
			String[] rooms = cl.getRooms();
			for (String room : rooms) {
				if (!conversations.containsKey(room))
					conversations.put(room, cl.isRoomPrivate(room));
			}
		}

		// Add room objects
		for (String room : conversations.keySet()) {
			convos.add(client.getServer().roomObject(room, conversations.get(room)));
		}

		res.add("conversations", convos);
		res.addProperty("eventId", "users.conversations");
		res.addProperty("success", true);
		client.sendPacket(res);

		return true;
	}

}
