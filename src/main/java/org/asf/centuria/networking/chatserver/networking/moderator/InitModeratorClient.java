package org.asf.centuria.networking.chatserver.networking.moderator;

import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.networking.AbstractChatPacket;
import org.asf.centuria.networking.gameserver.GameServer;

import com.google.gson.JsonObject;

public class InitModeratorClient extends AbstractChatPacket {

	@Override
	public String id() {
		return "centuria.moderatorclient.start";
	}

	@Override
	public AbstractChatPacket instantiate() {
		return new InitModeratorClient();
	}

	@Override
	public void parse(JsonObject data) {
	}

	@Override
	public void build(JsonObject data) {
	}

	@Override
	public boolean handle(ChatClient client) {
		// Check moderator client
		if (client.getObject(ModeratorClient.class) == null)
			return true;

		// Load perms
		String permLevel = "member";
		if (client.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
			permLevel = client.getPlayer().getSaveSharedInventory().getItem("permissions").getAsJsonObject()
					.get("permissionLevel").getAsString();
		}

		// Build message
		JsonObject response = new JsonObject();
		response.addProperty("eventId", "centuria.moderatorclient.start");
		response.addProperty("success", true);
		response.addProperty("permissionLevel", permLevel);

		// Check perms
		if (!GameServer.hasPerm(permLevel, "moderator")) {
			// Set error
			response.addProperty("success", false);

			// Send response
			client.sendPacket(response);
			return true;
		}

		// Send response
		client.addObject(new ModeratorClient());
		client.sendPacket(response);
		return true;
	}

}
