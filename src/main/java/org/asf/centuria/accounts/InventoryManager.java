package org.asf.centuria.accounts;

import org.asf.centuria.Centuria;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class InventoryManager {

	public static void addEmote(JsonArray item, String emote) {
		// Create emote json
		JsonObject obj = new JsonObject();
		obj.addProperty("defId", emote);
		JsonObject components = new JsonObject();
		JsonObject ts = new JsonObject();
		ts.addProperty("ts", System.currentTimeMillis());
		components.add("Timestamp", ts);
		obj.add("components", components);
		obj.addProperty("id", UUID.nameUUIDFromBytes(emote.getBytes()).toString());
		obj.addProperty("type", 9);
		item.add(obj);
	}

	public static void buildInventory(Player plr, PlayerInventory inv) {
		// Check if wings, mods and clothing is disabled
		if (!inv.getSaveSettings().giveAllMods && !inv.getSaveSettings().giveAllWings) {
			// Save item 2 as empty item
			inv.setItem("2", new JsonArray());
		}

		// Save item 100 and 102 as empty item
		inv.setItem("102", new JsonArray());
		inv.setItem("100", new JsonArray());

		// Add the starter bundle
		inv.getItemAccessor(null).add(25458);

		// Build avatars
		if (inv.getSaveSettings().giveAllAvatars) {
			// Unlock all avatars
			inv.getAvatarAccessor().unlockAvatarSpecies("Kitsune");
			inv.getAvatarAccessor().unlockAvatarSpecies("Senri");
			inv.getAvatarAccessor().unlockAvatarSpecies("Phoenix");
			inv.getAvatarAccessor().unlockAvatarSpecies("Dragon");
			inv.getAvatarAccessor().unlockAvatarSpecies("Kirin");
			inv.getAvatarAccessor().unlockAvatarSpecies("Fae");
			inv.getAvatarAccessor().unlockAvatarSpecies("Shinigami");
			inv.getAvatarAccessor().unlockAvatarSpecies("Werewolf");
			inv.getAvatarAccessor().unlockAvatarSpecies("Jackalope");
		} else {
			// Unlock Kitsune, Senri and Phoenix
			inv.getAvatarAccessor().unlockAvatarSpecies("Kitsune");
			inv.getAvatarAccessor().unlockAvatarSpecies("Senri");
			inv.getAvatarAccessor().unlockAvatarSpecies("Phoenix");
		}

		// Save changes
		for (String change : inv.getAccessor().getItemsToSave())
			inv.setItem(change, inv.getItem(change));
		inv.getAccessor().completedSave();
	}

	public static void fixBrokenAvatars(JsonArray item, PlayerInventory inv) {
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

		// Fix broken clothes
		for (JsonElement ele : item) {
			JsonObject ava = ele.getAsJsonObject().get("components").getAsJsonObject().get("AvatarLook")
					.getAsJsonObject().get("info").getAsJsonObject();
			if (ava.has("clothingItems")) {
				JsonArray items = ava.get("clothingItems").getAsJsonArray();
				for (JsonElement elem : items.deepCopy()) {
					JsonObject clothing = elem.getAsJsonObject();
					String id = clothing.get("itemInvID").getAsString();
					if (id.isEmpty() || inv.getClothingAccessor().getClothingData(id) == null) {
						// Fix it
						items.remove(clothing);
						changed = true;
					}
				}
			}
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
			Centuria.logger.info("Generating avatar file for " + plr.account.getDisplayName());
		InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
				.getResourceAsStream("content/avatars/avatars.json");
		JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
				.get("Avatars").getAsJsonObject();
		strm.close();

		// Construct the avatar list
		ArrayList<String> ids = new ArrayList<String>();
		for (String avatarSpecies : helper.keySet()) {
			JsonObject speciesData = helper.get(avatarSpecies).getAsJsonObject();
			if (plr != null)
				Centuria.logger.info("Generating avatar species object " + avatarSpecies + " for "
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
