package org.asf.centuria.networking.chatserver.networking;

import org.asf.centuria.networking.chatserver.ChatClient;

import com.google.gson.JsonObject;

public class PingPacket extends AbstractChatPacket {

	@Override
	public String id() {
		return "ping";
	}

	@Override
	public AbstractChatPacket instantiate() {
		return new PingPacket();
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
		res.addProperty("eventId", "ping");
		res.addProperty("success", true);
		client.sendPacket(res);

		return true;
	}

}
