package org.asf.emuferal.networking.chatserver.networking;

import java.util.ArrayList;
import java.util.UUID;

import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.dms.DMManager;
import org.asf.emuferal.networking.chatserver.ChatClient;
import org.asf.emuferal.social.SocialManager;

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
		if (!type.equals("private")) {
			// NOPE
			JsonObject res = new JsonObject();
			res.addProperty("eventId", "conversations.create");
			res.addProperty("success", false);
			client.sendPacket(res);
			return true;
		}

		// Load managers
		DMManager manager = DMManager.getInstance();
		AccountManager accounts = AccountManager.getInstance();

		// Participant list
		ArrayList<EmuFeralAccount> members = new ArrayList<EmuFeralAccount>();

		// Find participants and check privacy
		for (JsonElement participant : participants) {
			// TODO: privacy
			String id = participant.getAsString();

			// Block check
			if (SocialManager.getInstance().socialListExists(id)
					&& SocialManager.getInstance().getPlayerIsBlocked(id, client.getPlayer().getAccountID())) {

				// Send response
				JsonObject res = new JsonObject();
				res.addProperty("eventId", "conversations.create");
				res.addProperty("error", "blocked");
				res.addProperty("success", false);
				client.sendPacket(res);

				return true;
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
				EmuFeralAccount acc = accounts.getAccount(id);
				if (acc == null) {
					// Participant doesnt exist
					JsonObject res = new JsonObject();
					res.addProperty("error", "unrecognized_participant");
					res.addProperty("eventId", "conversations.create");
					res.addProperty("success", false);
					client.sendPacket(res);
					return true;
				} else {
					members.add(acc);
				}
			}
		}

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
		for (EmuFeralAccount participant : members) {
			// Load info
			if (!participant.getPlayerInventory().containsItem("dms"))
				participant.getPlayerInventory().setItem("dms", new JsonObject());
			JsonObject dms = participant.getPlayerInventory().getItem("dms").getAsJsonObject();

			// Save DM info
			for (EmuFeralAccount mem : members) {
				if (!mem.getAccountID().equals(participant.getAccountID())) {
					if (dms.has(mem.getAccountID()))
						dms.remove(mem.getAccountID());
					dms.addProperty(mem.getAccountID(), dmID);
				}
			}
			participant.getPlayerInventory().setItem("dms", dms);

			// Find online player
			for (ChatClient plr : client.getServer().getClients()) {
				if (plr.getPlayer().getAccountID().equals(participant.getAccountID())) {
					// Join room
					plr.joinRoom(dmID, true);

					// Send response
					plr.sendPacket(res);
					break;
				}
			}

		}

		return true;
	}

}
