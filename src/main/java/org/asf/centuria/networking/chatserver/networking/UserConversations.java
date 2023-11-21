package org.asf.centuria.networking.chatserver.networking;

import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.rooms.ChatRoomTypes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class UserConversations extends AbstractChatPacket {

	public int cursor;
	public int pageSize;

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
		pageSize = data.get("pageSize").getAsInt();
		if (data.has("cursor") && !data.get("cursor").getAsString().isEmpty())
			cursor = data.get("cursor").getAsInt();
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
		int cursorCurrent = cursor;
		int offset = cursorCurrent * pageSize;
		int indexInPage = 0;
		String[] rooms = client.getRooms();
		for (int i = offset; i < rooms.length; i++) {
			String room = rooms[i];
			if (client.getRoom(room).getType().equalsIgnoreCase(ChatRoomTypes.PRIVATE_CHAT)) {
				JsonObject obj = client.getServer().roomObject(room, ChatRoomTypes.PRIVATE_CHAT,
						client.getPlayer().getAccountID());
				if (obj != null)
					convos.add(obj);
				indexInPage++;
				if (indexInPage >= pageSize)
					break;
			}
		}

		res.add("conversations", convos);
		res.addProperty("eventId", "users.conversations");
		if ((cursorCurrent + 1) * pageSize < rooms.length)
			res.addProperty("cursor", cursorCurrent + 1);
		res.addProperty("success", true);
		client.sendPacket(res);

		return true;
	}

}
