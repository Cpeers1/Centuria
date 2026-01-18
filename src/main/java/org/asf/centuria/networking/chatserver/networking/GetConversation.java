package org.asf.centuria.networking.chatserver.networking;

import java.util.UUID;

import org.asf.centuria.dms.DMManager;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.rooms.ChatRoom;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.social.SocialManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class GetConversation extends AbstractChatPacket {

	private String convo;
	private static String NIL_UUID = new UUID(0, 0).toString();

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
		// System messages
		if (convo.equals(NIL_UUID)) {
			// Build object
			JsonObject roomData = new JsonObject();
			roomData.addProperty("conversation_id", NIL_UUID);
			roomData.addProperty("title", "Centuria Server messages");

			// Build participants object
			JsonArray members = new JsonArray();
			members.add(NIL_UUID);
			members.add(client.getPlayer().getAccountID());
			roomData.add("participants", members);
			roomData.addProperty("conversationType", "private");

			// Send packet
			JsonObject res = new JsonObject();
			res.add("conversation", roomData);
			res.addProperty("eventId", "conversations.get");
			res.addProperty("success", true);
			client.sendPacket(res);
			return true;
		}

		// Check if dm
		if (DMManager.getInstance().dmExists(convo)) {
			// Check moderator perms
			String permLevel = "member";
			if (client.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
				permLevel = client.getPlayer().getSaveSharedInventory().getItem("permissions").getAsJsonObject()
						.get("permissionLevel").getAsString();
			}

			// Check if all the others blocked the member
			boolean hasNonBlocked = false;
			for (String p2 : DMManager.getInstance().getDMParticipants(convo)) {
				if (!p2.equals(client.getPlayer().getAccountID()) && !p2.startsWith("plaintext:")) {
					if (!SocialManager.getInstance().socialListExists(p2)
							|| !SocialManager.getInstance().getPlayerIsBlocked(p2, client.getPlayer().getAccountID()))
						hasNonBlocked = true;
				}
			}
			if (!hasNonBlocked && !GameServer.hasPerm(permLevel, "moderator")) {
				// Fail
				JsonObject res = new JsonObject();
				res.addProperty("eventId", "conversations.get");
				res.addProperty("success", false);
				client.sendPacket(res);
				return true;
			}
		}

		// Get room
		ChatRoom room = client.getRoom(convo);

		// Send response
		JsonObject res = new JsonObject();
		res.add("conversation", client.getServer().roomObject(convo, room != null ? room.getType() : "room",
				client.getPlayer().getAccountID()));
		res.addProperty("eventId", "conversations.get");
		res.addProperty("success", true);
		client.sendPacket(res);

		return true;
	}

}
