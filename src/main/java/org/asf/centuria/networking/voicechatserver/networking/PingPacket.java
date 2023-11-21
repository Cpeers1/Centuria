package org.asf.centuria.networking.voicechatserver.networking;

import org.asf.centuria.networking.voicechatserver.VoiceChatClient;

import com.google.gson.JsonObject;

public class PingPacket extends AbstractVoiceChatPacket {

	@Override
	public String id() {
		return "ping";
	}

	@Override
	public AbstractVoiceChatPacket instantiate() {
		return new PingPacket();
	}

	@Override
	public void parse(JsonObject data) {
	}

	@Override
	public void build(JsonObject data) {
	}

	@Override
	public boolean handle(VoiceChatClient client) {
		// Send response
		JsonObject res = new JsonObject();
		res.addProperty("eventId", "ping");
		res.addProperty("success", true);
		client.sendPacket(res);

		return true;
	}

}
