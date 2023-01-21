package org.asf.centuria.networking.chatserver.networking;

import org.asf.centuria.dms.DMManager;
import org.asf.centuria.networking.chatserver.ChatClient;

import com.google.gson.JsonObject;

public class JoinRoomPacket extends AbstractChatPacket {

	public String room;
	public String participant;

	@Override
	public String id() {
		return "conversations.addParticipant";
	}

	@Override
	public AbstractChatPacket instantiate() {
		return new JoinRoomPacket();
	}

	@Override
	public void parse(JsonObject data) {
		room = data.get("conversationId").getAsString();
		participant = data.get("participant").getAsString();
	}

	@Override
	public void build(JsonObject data) {
	}

	@Override
	public boolean handle(ChatClient cCl) {
		// Find client
		ChatClient client = cCl;
		if (!participant.equals(client.getPlayer().getAccountID()))
			return true;

		// Check if its a dm
		DMManager manager = DMManager.getInstance();
		if (manager.dmExists(room)) {
			// Ignore join
			JsonObject res = new JsonObject();
			res.addProperty("eventId", "conversations.addParticipant");
			res.addProperty("success", false);
			client.sendPacket(res);
			return true;
		}

		// Leave old public room
		for (String room : client.getRooms()) {
			if (!client.isRoomPrivate(room))
				client.leaveRoom(room);
		}

		// Send response
		JsonObject res = new JsonObject();
		res.addProperty("conversationId", room);
		res.addProperty("participant", participant);
		res.addProperty("eventId", "conversations.addParticipant");
		res.addProperty("success", true);
		client.sendPacket(res);

		// Check online player
		if (cCl.getPlayer().getOnlinePlayerInstance() != null)
			cCl.getPlayer().getOnlinePlayerInstance().wasInChat = true;

		// Join room
		if (!client.isInRoom(room))
			client.joinRoom(room, false);

		return true;
	}

}
