package org.asf.centuria.networking.voicechatserver.networking;

import org.asf.centuria.networking.voicechatserver.VoiceChatClient;
import org.asf.centuria.social.SocialManager;

import com.google.gson.JsonObject;

public class AddParticipantToCallPacket extends AbstractVoiceChatPacket {

	public String playerID = "";

	@Override
	public String id() {
		return "call.add";
	}

	@Override
	public AbstractVoiceChatPacket instantiate() {
		return new AddParticipantToCallPacket();
	}

	@Override
	public void parse(JsonObject data) {
		playerID = data.get("attendee").getAsString();
	}

	@Override
	public void build(JsonObject data) {
	}

	@Override
	public boolean handle(VoiceChatClient client) {
		// Find target
		VoiceChatClient target = client.getServer().getClient(playerID);
		if (target == null) {
			// Error
			JsonObject res = new JsonObject();
			res.addProperty("eventId", "call.add");
			res.addProperty("error", "not_online");
			res.addProperty("success", false);
			client.sendPacket(res);
			return true;
		}

		// Check if the sender has blocked this receiver
		SocialManager socialManager = SocialManager.getInstance();
		if (socialManager.getPlayerIsBlocked(client.getPlayer().getAccountID(), target.getPlayer().getAccountID())) {
			// Blocked
			JsonObject res = new JsonObject();
			res.addProperty("eventId", "call.add");
			res.addProperty("error", "not_online");
			res.addProperty("success", false);
			client.sendPacket(res);
			return true;
		}

		// Load voice chat social system settings
		String privacy = target.getPlayer().getPrivacySettings().get("voice_chat").getAsString();
		if (privacy.equals("nobody")) {
			// Blocked
			JsonObject res = new JsonObject();
			res.addProperty("eventId", "call.add");
			res.addProperty("error", "blocked");
			res.addProperty("success", false);
			client.sendPacket(res);
			return true;
		} else if (privacy.equals("following")
				&& (!socialManager.socialListExists(target.getPlayer().getAccountID()) || !socialManager
						.getPlayerIsFollowing(target.getPlayer().getAccountID(), client.getPlayer().getAccountID()))) {
			// Blocked
			JsonObject res = new JsonObject();
			res.addProperty("eventId", "call.add");
			res.addProperty("error", "blocked");
			res.addProperty("success", false);
			client.sendPacket(res);
			return true;
		}

		// Send success
		JsonObject res = new JsonObject();
		res.addProperty("eventId", "call.add");
		res.addProperty("success", true);
		client.sendPacket(res);

		return true;
	}

}
