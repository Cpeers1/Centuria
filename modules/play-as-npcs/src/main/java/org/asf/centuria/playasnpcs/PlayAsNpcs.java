package org.asf.centuria.playasnpcs;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.modules.ICenturiaModule;
import org.asf.centuria.modules.eventbus.EventListener;
import org.asf.centuria.modules.events.chatcommands.ChatCommandEvent;
import org.asf.centuria.modules.events.chatcommands.ModuleCommandSyntaxListEvent;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemRemovedPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class PlayAsNpcs implements ICenturiaModule {

	private HashMap<String, JsonObject> npcInfos = new HashMap<String, JsonObject>();

	@Override
	public String id() {
		return "playasnpcs";
	}

	@Override
	public String version() {
		return "1.0.0.A1";
	}

	@Override
	public void init() {
		// Load the helper
		try {
			InputStream strm = getClass().getClassLoader().getResourceAsStream("npcs.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("NpcData").getAsJsonObject();
			strm.close();

			// Parse
			helper.keySet().forEach(t -> {
				npcInfos.put(t, helper.get(t).getAsJsonObject());
			});
		} catch (JsonSyntaxException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@EventListener
	public void registerCommands(ModuleCommandSyntaxListEvent event) {
		if (event.hasPermission("moderator")) {
			event.addCommandSyntaxMessage("addnpcavatar <defid>");
			event.addCommandSyntaxMessage("clearnpcavatars");
		}
	}

	@EventListener
	public void handleCommands(ChatCommandEvent event) {
		if (event.hasPermission("moderator") && event.getCommandID().equals("addnpcavatar")) {
			event.setHandled();
			if (event.getCommandArguments().length < 1) {
				event.respond("Missing argument: NPC defID");
				return;
			}

			// Find NPC
			if (!npcInfos.containsKey(event.getCommandArguments()[0])) {
				event.respond("Invalid argument: NPC defID: ID not recognized");
				return;
			}

			// Get avatar info
			JsonObject ava = npcInfos.get(event.getCommandArguments()[0]);

			// Clothing
			JsonArray clothing = ava.get("clothingItems").getAsJsonArray();
			for (JsonElement ele : clothing) {
				JsonObject clothingItem = ele.getAsJsonObject();
				clothingItem.remove("itemInvID");
				clothingItem.addProperty("itemInvID", "-1");
			}

			// Save avatar to inventory
			Player plr = event.getAccount().getOnlinePlayerInstance();
			JsonArray items = plr.account.getPlayerInventory().getItem("avatars").getAsJsonArray();
			JsonObject ts = new JsonObject();
			ts.addProperty("ts", System.currentTimeMillis());
			JsonObject nm = new JsonObject();
			nm.addProperty("name", "NPC");
			JsonObject al = new JsonObject();
			al.addProperty("gender", 0);
			al.add("info", ava);
			JsonObject components = new JsonObject();
			components.add("Timestamp", ts);
			components.add("AvatarLook", al);
			components.add("PrimaryLook", new JsonObject());
			components.add("Name", nm);
			JsonObject avatar = new JsonObject();
			avatar.addProperty("defId", 8254);
			avatar.add("components", components);
			avatar.addProperty("id", UUID.randomUUID().toString());
			avatar.addProperty("type", 200);
			items.add(avatar);

			// Save
			event.getAccount().getPlayerInventory().setItem("avatars", items);
			InventoryItemPacket pk = new InventoryItemPacket();
			pk.item = items;
			plr.client.sendPacket(pk);
			event.respond("Avatar added to inventory");
		} else if (event.hasPermission("moderator") && event.getCommandID().equals("clearnpcavatars")) {
			event.setHandled();

			// Update inventory
			Player plr = event.getAccount().getOnlinePlayerInstance();
			ArrayList<String> removedItems = new ArrayList<String>();
			ArrayList<JsonObject> removed = new ArrayList<JsonObject>();
			JsonArray items = plr.account.getPlayerInventory().getItem("avatars").getAsJsonArray();
			for (JsonElement ele : items) {
				JsonObject avatar = ele.getAsJsonObject();
				JsonObject info = avatar.get("components").getAsJsonObject().get("AvatarLook").getAsJsonObject()
						.get("info").getAsJsonObject();
				if (avatar.get("defId").getAsInt() == 8254) {
					// Kitsune slot
					if (info.get("actorClassDefID").getAsInt() != 1929) {
						// Not a kitsune probably a npc so delete it
						removed.add(avatar);
						removedItems.add(avatar.get("id").getAsString());
					}
				}
			}
			for (JsonObject remove : removed)
				items.remove(remove);
			InventoryItemRemovedPacket pkt = new InventoryItemRemovedPacket();
			pkt.items = removedItems.toArray(new String[0]);
			event.getAccount().getOnlinePlayerInstance().client.sendPacket(pkt);

			// Save
			event.getAccount().getPlayerInventory().setItem("avatars", items);
			event.respond("Avatars updated");
		}
	}

}
