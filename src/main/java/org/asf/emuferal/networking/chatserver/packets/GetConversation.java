package org.asf.emuferal.networking.chatserver.packets;

import org.asf.emuferal.networking.chatserver.ChatClient;

import com.google.gson.JsonObject;

public class GetConversation extends AbstractChatPacket {

	private String convo;

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
		// Send response
		JsonObject res = new JsonObject();
		res.add("conversation", client.getServer().roomObject(convo, client.isRoomPrivate(convo)));
		res.addProperty("eventId", "conversations.get");
		res.addProperty("success", true);
		client.sendPacket(res);

		return true;
	}

}
