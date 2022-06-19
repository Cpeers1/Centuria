package org.asf.emuferal.accounts.highlevel.impl;

import java.io.IOException;
import java.io.InputStream;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.AvatarAccessor;
import org.asf.emuferal.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class AvatarAccessorImpl extends AvatarAccessor {

	private static JsonObject helper;
	private static JsonObject defaultsHelper;
	static {
		try {
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/avatarhelper.json");
			helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject().get("Avatars")
					.getAsJsonObject();
			strm.close();

			strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/avatardefaultshelper.json");
			defaultsHelper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("AvatarDefaults").getAsJsonObject();
			strm.close();
		} catch (JsonSyntaxException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public AvatarAccessorImpl(PlayerInventory inventory) {
		super(inventory);
	}

	@Override
	public void addExtraLookSlot() {
		if (!inventory.containsItem("avatars")) {
			inventory.setItem("avatars", new JsonArray());
		}

		// Add a new look slot for each creature
		JsonArray avatars = inventory.getItem("avatars").getAsJsonArray();
		for (JsonElement elem : avatars) {
			JsonObject avatar = elem.getAsJsonObject();

			// Make sure to only do this for a primary look
			if (avatar.get("components").getAsJsonObject().has("PrimaryLook")) {
				// Create the slot

				String type = avatar.get("defId").getAsString();
				// Translate defID to a type
				JsonObject speciesData = helper.get(type).getAsJsonObject();
				for (String species : helper.keySet()) {
					JsonObject ava = helper.get(species).getAsJsonObject();
					if (ava.get("defId").getAsString().equals(type)) {
						type = species;
						break;
					}
				}

				// Name
				JsonObject nm = new JsonObject();
				nm.addProperty("name", "");

				// Avatar info
				JsonObject al = new JsonObject();
				al.addProperty("gender", 0);
				al.add("info", speciesData.get("info").getAsJsonObject());

				// Add the look slot
				inventory.getAccessor().createInventoryObject("avatars", 200, speciesData.get("defId").getAsInt(),
						new ItemComponent("AvatarLook", al), new ItemComponent("Name", nm));
			}
		}

		// Mark what files to save
		addItemToSave("avatars");
	}

	@Override
	public void unlockAvatarSpecies(String type) {
		if (isAvatarSpeciesUnlocked(type))
			return;

		// Translate defID to a type
		for (String species : helper.keySet()) {
			JsonObject ava = helper.get(species).getAsJsonObject();
			if (ava.get("defId").getAsString().equals(type)) {
				type = species;
				break;
			}
		}

		// Add the species looks
		JsonObject speciesData = helper.get(type).getAsJsonObject();
		int actorDefID = speciesData.get("info").getAsJsonObject().get("actorClassDefID").getAsInt();
		if (helper.has(type)) {
			if (!inventory.containsItem("avatars")) {
				inventory.setItem("avatars", new JsonArray());
			}

			// Find the save slot count
			JsonArray avatars = inventory.getItem("avatars").getAsJsonArray();
			int slots = 0;
			for (JsonElement ele : avatars) {
				JsonObject ava = ele.getAsJsonObject();
				String dID = ava.get("defId").getAsString();
				if (ava.get("components").getAsJsonObject().has("PrimaryLook")) {
					// Find other looks
					for (JsonElement ele2 : avatars) {
						JsonObject ava2 = ele2.getAsJsonObject();
						String dID2 = ava2.get("defId").getAsString();
						if (!ava2.get("components").getAsJsonObject().has("PrimaryLook") && dID.equals(dID2)) {
							slots++;
						}
					}
					break;
				}
			}
			if (slots == 0)
				slots = 12;

			// Find the current save slot count
			int cSlots = 0;
			for (JsonElement ele : avatars) {
				JsonObject ava = ele.getAsJsonObject();
				String dID = ava.get("defId").getAsString();
				if (ava.get("components").getAsJsonObject().has("PrimaryLook") && dID.equals(type)) {
					// Find other looks
					for (JsonElement ele2 : avatars) {
						JsonObject ava2 = ele2.getAsJsonObject();
						String dID2 = ava2.get("defId").getAsString();
						if (!ava2.get("components").getAsJsonObject().has("PrimaryLook") && dID.equals(dID2)) {
							cSlots++;
						}
					}
					break;
				}
			}
			if (slots == 0)
				slots = 12;

			// Add the look files and scan in the ID
			if (cSlots < slots) {
				if (cSlots == 0) {
					// Add primary

					// Name
					JsonObject nm = new JsonObject();
					nm.addProperty("name", "");

					// Avatar info
					JsonObject al = new JsonObject();
					al.addProperty("gender", 0);
					al.add("info", speciesData.get("info").getAsJsonObject());

					// Add the look slot
					inventory.getAccessor().createInventoryObject("avatars", 200, speciesData.get("defId").getAsInt(),
							new ItemComponent("PrimaryLook", new JsonObject()), new ItemComponent("AvatarLook", al),
							new ItemComponent("Name", nm));
				}

				// Add slots
				for (int i = cSlots; i < slots; i++) {
					// Name
					JsonObject nm = new JsonObject();
					nm.addProperty("name", "");

					// Avatar info
					JsonObject al = new JsonObject();
					al.addProperty("gender", 0);
					al.add("info", speciesData.get("info").getAsJsonObject());

					// Add the look slot
					inventory.getAccessor().createInventoryObject("avatars", 200, speciesData.get("defId").getAsInt(),
							new ItemComponent("AvatarLook", al), new ItemComponent("Name", nm));
				}
			}

			// Unlock all mods for this species
			if (defaultsHelper.has(Integer.toString(actorDefID))) {
				defaultsHelper.get(Integer.toString(actorDefID)).getAsJsonArray().forEach(item -> {
					int id = item.getAsInt();
					if (!isAvatarPartUnlocked(id))
						unlockAvatarPart(id);
				});
			}

			// Update the species list
			if (!inventory.getAccessor().hasInventoryObject("1", actorDefID)) {
				// Add species
				inventory.getAccessor().createInventoryObject("1", actorDefID);
			}
		}
	}

	@Override
	public boolean isAvatarSpeciesUnlocked(String type) {
		if (!inventory.containsItem("1"))
			return false;

		String defID = type;

		// Translate type to a defID
		if (helper.has(defID)) {
			defID = helper.get(defID).getAsJsonObject().get("defId").getAsString();
		}

		// Find species
		if (defID.matches("^[0-9]+$"))
			return inventory.getAccessor().hasInventoryObject("avatars", Integer.parseInt(defID));
		return false;
	}

	@Override
	public boolean isAvatarPartUnlocked(int defID) {
		return inventory.getAccessor().hasInventoryObject("2", defID);
	}

	@Override
	public void unlockAvatarPart(int defID) {
		if (isAvatarPartUnlocked(defID))
			return;

		// Unlock part
		inventory.getAccessor().createInventoryObject("2", defID);
	}

	@Override
	public void lockAvatarPart(int defID) {
		// Lock part
		inventory.getAccessor().removeInventoryObject("2", defID);
	}
}
