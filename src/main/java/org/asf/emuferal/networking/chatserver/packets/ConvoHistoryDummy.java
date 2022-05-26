package org.asf.emuferal.networking.chatserver.packets;

import org.asf.emuferal.networking.chatserver.ChatClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ConvoHistoryDummy extends AbstractChatPacket {

	@Override
	public String id() {
		return "conversations.history";
	}

	@Override
	public AbstractChatPacket instantiate() {
		return new ConvoHistoryDummy();
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
		res.add("messages", new JsonArray());
		res.addProperty("eventId", "conversations.history");
		res.addProperty("success", true);
		client.sendPacket(res);
		return true;
	}

}
