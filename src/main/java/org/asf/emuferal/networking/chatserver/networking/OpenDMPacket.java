package org.asf.emuferal.networking.chatserver.networking;

import org.asf.emuferal.dms.DMManager;
import org.asf.emuferal.networking.chatserver.ChatClient;
import org.asf.emuferal.social.SocialManager;

import com.google.gson.JsonObject;

public class OpenDMPacket extends AbstractChatPacket {

	private String participant;

	@Override
	public String id() {
		return "conversations.openPrivate";
	}

	@Override
	public AbstractChatPacket instantiate() {
		return new OpenDMPacket();
	}

	@Override
	public void parse(JsonObject data) {
		participant = data.get("participant").getAsString();
	}

	@Override
	public void build(JsonObject data) {
	}

	@Override
	public boolean handle(ChatClient client) {
		// Block check
		if (SocialManager.getInstance().socialListExists(participant)
				&& SocialManager.getInstance().getPlayerIsBlocked(participant, client.getPlayer().getAccountID())) {

			// Send response
			JsonObject res = new JsonObject();
			res.addProperty("eventId", "conversations.openPrivate");
			res.addProperty("error", "blocked");
			res.addProperty("success", false);
			client.sendPacket(res);

			return true;
		}

		// Load data
		DMManager manager = DMManager.getInstance();
		if (!client.getPlayer().getPlayerInventory().containsItem("dms"))
			client.getPlayer().getPlayerInventory().setItem("dms", new JsonObject());
		JsonObject dms = client.getPlayer().getPlayerInventory().getItem("dms").getAsJsonObject();

		// Find DM
		if (dms.has(participant) && manager.dmExists(dms.get(participant).getAsString())) {
			// Send response
			JsonObject res = new JsonObject();
			res.addProperty("eventId", "conversations.openPrivate");
			res.addProperty("conversationId", dms.get(participant).getAsString());
			res.addProperty("success", true);
			client.sendPacket(res);
			return true;
		}

		// Send response
		JsonObject res = new JsonObject();
		res.addProperty("eventId", "conversations.openPrivate");
		res.addProperty("error", "not_found");
		res.addProperty("success", false);
		client.sendPacket(res);

		return true;
	}

}
