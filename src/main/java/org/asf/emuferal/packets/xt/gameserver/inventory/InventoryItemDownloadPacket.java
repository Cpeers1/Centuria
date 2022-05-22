package org.asf.emuferal.packets.xt.gameserver.inventory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import org.asf.emuferal.EmuFeral;
import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class InventoryItemDownloadPacket implements IXtPacket<InventoryItemDownloadPacket> {

	private String slot = "";

	@Override
	public InventoryItemDownloadPacket instantiate() {
		return new InventoryItemDownloadPacket();
	}

	@Override
	public String id() {
		return "ilt";
	}

	@Override
	public void parse(XtReader reader) {
		slot = reader.read();
	}

	@Override
	public void build(XtWriter writer) {
		writer.writeString(slot);
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		Player plr = (Player) client.container;
		PlayerInventory inv = plr.account.getPlayerInventory();

		// Check if inventory is built
		if (!inv.containsItem("1")) {
			buildInventory(plr, inv);
		}

		// Load the item
		JsonElement item = inv.getItem(slot.equals("200") ? "avatars" : slot);

		// Repair broken avatars
		if (slot.equals("200")) {
			fixBrokenAvatars(item.getAsJsonArray(), inv);
		}

		// Send the item to the client
		InventoryItemPacket pkt = new InventoryItemPacket();
		pkt.item = item;
		client.sendPacket(pkt);

		return true;
	}

	private void buildInventory(Player plr, PlayerInventory inv) {
		// Build avatars
		if (EmuFeral.giveAllAvatars) {
			// Unlock all avatars
			inv.getAccessor().unlockAvatarSpecies("Kitsune");
			inv.getAccessor().unlockAvatarSpecies("Senri");
			inv.getAccessor().unlockAvatarSpecies("Phoenix");
			inv.getAccessor().unlockAvatarSpecies("Dragon");
			inv.getAccessor().unlockAvatarSpecies("Kirin");
			inv.getAccessor().unlockAvatarSpecies("Fae");
			inv.getAccessor().unlockAvatarSpecies("Shinigami");
			inv.getAccessor().unlockAvatarSpecies("Werewolf");
			inv.getAccessor().unlockAvatarSpecies("Jackalope");
		} else {
			// Unlock Kitsune and Senri
			inv.getAccessor().unlockAvatarSpecies("Kitsune");
			inv.getAccessor().unlockAvatarSpecies("Senri");
		}

		
		
		// Save changes
		for (String change : inv.getAccessor().getItemsToSave())
			inv.setItem(change, inv.getItem(change));
		inv.getAccessor().completedSave();
	}

	private void fixBrokenAvatars(JsonArray item, PlayerInventory inv) {
		boolean changed = false;

		// Fix unlisted
		for (JsonElement ele : item) {
			JsonObject ava = ele.getAsJsonObject();
			int defID = ava.get("defId").getAsInt();
			boolean hasPrimary = false;

			for (JsonElement ele2 : item) {
				JsonObject ava2 = ele2.getAsJsonObject();
				int defID2 = ava2.get("defId").getAsInt();
				if (defID == defID2 && ava2.get("components").getAsJsonObject().has("PrimaryLook")) {
					hasPrimary = true;
					break;
				}
			}

			// Fix it
			if (!hasPrimary)
				ava.get("components").getAsJsonObject().add("PrimaryLook", new JsonObject());
			changed = true;
		}

		// Fix look slots
		for (JsonElement ele : item.deepCopy()) {
			JsonObject ava = ele.getAsJsonObject();
			if (ava.get("components").getAsJsonObject().has("PrimaryLook"))
				continue;

			int defID = ava.get("defId").getAsInt();
			int lookCount = 0;

			for (JsonElement ele2 : item) {
				JsonObject ava2 = ele2.getAsJsonObject();
				int defID2 = ava2.get("defId").getAsInt();
				if (defID == defID2 && !ava2.get("components").getAsJsonObject().has("PrimaryLook")) {
					lookCount++;
					break;
				}
			}

			// Fix it
			if (lookCount > 12)
				item.remove(ava);
			changed = true;
		}

		// Save
		if (changed) {
			inv.setItem("avatars", item);
		}
	}

	public static JsonArray buildDefaultLooksFile(Player plr) throws IOException {
		JsonArray items = new JsonArray();

		// Load the helper from resources
		if (plr != null)
			System.out.println("Generating avatar file for " + plr.account.getDisplayName());
		InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
				.getResourceAsStream("defaultitems/avatarhelper.json");
		JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
				.get("Avatars").getAsJsonObject();
		strm.close();

		// Construct the avatar list
		ArrayList<String> ids = new ArrayList<String>();
		for (String avatarSpecies : helper.keySet()) {
			JsonObject speciesData = helper.get(avatarSpecies).getAsJsonObject();
			if (plr != null)
				System.out.println("Generating avatar species object " + avatarSpecies + " for "
						+ plr.account.getDisplayName() + "...");

			// Build 11 look files and set the first to primary
			boolean primary = true;
			for (int i = 0; i < 13; i++) {
				// Generate look ID
				String lID = UUID.randomUUID().toString();
				while (ids.contains(lID))
					lID = UUID.randomUUID().toString();
				ids.add(lID);

				// Timestamp
				JsonObject ts = new JsonObject();
				ts.addProperty("ts", System.currentTimeMillis());

				// Name
				JsonObject nm = new JsonObject();
				nm.addProperty("name", "");

				// Avatar info
				JsonObject al = new JsonObject();
				al.addProperty("gender", 0);
				al.add("info", speciesData.get("info").getAsJsonObject());

				// Build components
				JsonObject components = new JsonObject();
				if (primary)
					components.add("PrimaryLook", new JsonObject());
				components.add("Timestamp", ts);
				components.add("AvatarLook", al);
				components.add("Name", nm);

				// Build data container
				JsonObject lookObj = new JsonObject();
				lookObj.addProperty("defId", speciesData.get("defId").getAsInt());
				lookObj.add("components", components);
				lookObj.addProperty("id", lID);
				lookObj.addProperty("type", 200);

				// Add the avatar
				items.add(lookObj);
				primary = false;
			}
		}

		return items;
	}

}
