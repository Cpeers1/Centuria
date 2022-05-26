package org.asf.emuferal.networking.chatserver.packets;

import org.asf.emuferal.networking.chatserver.ChatClient;

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
		ChatClient client = null;
		for (ChatClient cl : cCl.getServer().getClients()) {
			if (cl.getPlayer().getAccountID().equals(participant)) {
				client = cl;
				break;
			}
		}
		if (client == null)
			return true;

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

		// Broadcast join
		for (ChatClient cl : cCl.getServer().getClients())
			if (cl != client && cl.isInRoom(room))
				cl.sendPacket(res);

		// Join room
		client.joinRoom(room, false); // TODO: private chat support

		return true;
	}

}
