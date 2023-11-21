package org.asf.centuria.networking.chatserver.networking;

import java.util.ArrayList;
import java.util.UUID;

import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.dms.DMManager;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.rooms.ChatRoomTypes;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.social.SocialManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CreateConversationPacket extends AbstractChatPacket {

	private JsonArray participants;
	private String type;

	@Override
	public String id() {
		return "conversations.create";
	}

	@Override
	public AbstractChatPacket instantiate() {
		return new CreateConversationPacket();
	}

	@Override
	public void parse(JsonObject data) {
		participants = data.get("participants").getAsJsonArray();
		type = data.get("conversationType").getAsString();
	}

	@Override
	public void build(JsonObject data) {
	}

	@Override
	public boolean handle(ChatClient client) {

		try {
			switch (type) {
			case "private": {
				createPrivateChat(client);
				break;
			}
			case "transient": {
				createTransientChat(client);
				break;
			}
			default: {
				// NOPE
				JsonObject res = new JsonObject();
				res.addProperty("eventId", "conversations.create");
				res.addProperty("success", false);
				client.sendPacket(res);
				break;
			}
			}
		} catch (Exception e) {
			e.printStackTrace();

			JsonObject res = new JsonObject();
			res.addProperty("eventId", "conversations.create");
			res.addProperty("success", false);
			client.sendPacket(res);
		}

		return true;
	}

	public void createPrivateChat(ChatClient client) {
		// Load managers
		DMManager manager = DMManager.getInstance();
		AccountManager accounts = AccountManager.getInstance();

		ArrayList<CenturiaAccount> members = getParticipants(client, accounts);

		if (members == null)
			return;

		// Create conversation ID
		String dmID = UUID.randomUUID().toString();
		while (manager.dmExists(dmID))
			dmID = UUID.randomUUID().toString();

		// Open DM
		manager.openDM(dmID, members.stream().map(t -> t.getAccountID()).toArray(t -> new String[t]));

		// Build response
		JsonObject res = new JsonObject();
		res.addProperty("conversationId", dmID);
		res.addProperty("eventId", "conversations.create");
		res.addProperty("success", true);

		// Open DM for all participants
		for (CenturiaAccount participant : members) {
			// Load info
			if (!participant.getSaveSharedInventory().containsItem("dms"))
				participant.getSaveSharedInventory().setItem("dms", new JsonObject());
			JsonObject dms = participant.getSaveSharedInventory().getItem("dms").getAsJsonObject();

			// Save DM info
			for (CenturiaAccount mem : members) {
				if (!mem.getAccountID().equals(participant.getAccountID())) {
					if (dms.has(mem.getAccountID()))
						dms.remove(mem.getAccountID());
					dms.addProperty(mem.getAccountID(), dmID);
				}
			}
			participant.getSaveSharedInventory().setItem("dms", dms);

			// Find online player
			for (ChatClient plr : client.getServer().getClients()) {
				if (plr.getPlayer().getAccountID().equals(participant.getAccountID())) {
					// Join room
					plr.joinRoom(dmID, ChatRoomTypes.PRIVATE_CHAT);

					// Send response
					plr.sendPacket(res);
					break;
				}
			}
		}
	}

	public void createTransientChat(ChatClient client) {
		// Transient chats are similar to dm's, but different.
		// Mostly used for trade chat.

		// Managers
		AccountManager accounts = AccountManager.getInstance();

		// Participant list
		ArrayList<CenturiaAccount> members = getParticipants(client, accounts);

		if (members == null)
			return;

		// Create conversation ID
		String rID = "t_"
				+ (participants.size() == 0 ? client.getPlayer().getAccountID() : participants.get(0).getAsString());

		for (var member : members) {
			// If the members have active trades..
			if (member.getOnlinePlayerInstance() != null && member.getOnlinePlayerInstance().tradeEngagedIn != null) {
				// Set their trade's chat ID to this new conversation ID.
				member.getOnlinePlayerInstance().tradeEngagedIn.chatConversationId = rID;
			}
		}

		// Build response
		JsonObject res = new JsonObject();
		res.addProperty("conversationId", rID);
		res.addProperty("eventId", "conversations.create");
		res.addProperty("success", true);

		// Open DM for all participants
		for (CenturiaAccount participant : members) {
			// Find online player
			for (ChatClient plr : client.getServer().getClients()) {
				if (plr.getPlayer().getAccountID().equals(participant.getAccountID())) {
					// Join room
					plr.joinRoom(rID, ChatRoomTypes.TRANSIENT_CHAT);

					// Send response
					plr.sendPacket(res);
					break;
				}
			}
		}
	}

	private ArrayList<CenturiaAccount> getParticipants(ChatClient client, AccountManager accounts) {
		// Participant list
		ArrayList<CenturiaAccount> members = new ArrayList<CenturiaAccount>();

		// Find participants and check block
		for (JsonElement participant : participants) {
			String id = participant.getAsString();

			// Get account
			CenturiaAccount acc = accounts.getAccount(id);
			if (acc != null) {
				// Get permissions of account
				String permLevel = "member";
				if (client.getPlayer().getSaveSharedInventory().containsItem("permissions")) {
					permLevel = client.getPlayer().getSaveSharedInventory().getItem("permissions").getAsJsonObject()
							.get("permissionLevel").getAsString();
				}

				// Block check
				if (SocialManager.getInstance().socialListExists(id)
						&& SocialManager.getInstance().getPlayerIsBlocked(id, client.getPlayer().getAccountID())
						&& !GameServer.hasPerm(permLevel, "moderator")) {
					// Send response
					JsonObject res = new JsonObject();
					res.addProperty("eventId", "conversations.create");
					res.addProperty("error", "not_allowed");
					res.addProperty("success", false);
					client.sendPacket(res);
					return null;
				}

				// Find online player
				boolean found = false;
				for (ChatClient plr : client.getServer().getClients()) {
					if (plr.getPlayer().getAccountID().equals(id)) {
						members.add(plr.getPlayer());
						found = true;
						break;
					}
				}

				// Find offline player
				if (!found) {
					members.add(acc);
				}
			} else {
				// Participant doesnt exist
				JsonObject res = new JsonObject();
				res.addProperty("error", "unrecognized_participant");
				res.addProperty("eventId", "conversations.create");
				res.addProperty("success", false);
				client.sendPacket(res);
				return null;
			}
		}
		return members;
	}

}
