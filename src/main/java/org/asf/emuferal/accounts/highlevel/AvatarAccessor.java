package org.asf.emuferal.accounts.highlevel;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class AvatarAccessor {
	private PlayerInventory inventory;

	public AvatarAccessor(PlayerInventory inventory) {
		this.inventory = inventory;
	}

	/**
	 * Call this after saving items
	 */
	public void completedSave() {
		inventory.getAccessor().itemsToSave.clear();
	}

	/**
	 * Retrieves which items to save
	 * 
	 * @return Array of item IDs to save
	 */
	public String[] getItemsToSave() {
		return inventory.getAccessor().getItemsToSave();
	}

	/**
	 * Gives the player another look slot
	 */
	public void addExtraLookSlot() {
		if (!inventory.containsItem("avatars")) {
			inventory.setItem("avatars", new JsonArray());
		}

		// Add a new look slot for each creature
		JsonArray avatars = inventory.getItem("avatars").getAsJsonArray();
		for (JsonElement elem : avatars) {
			JsonObject avatar = elem.getAsJsonObject();

			try {
				// Make sure to only do this for a primary look
				if (avatar.get("components").getAsJsonObject().has("PrimaryLook")) {
					// Create the slot

					// Generate look ID
					String lID = UUID.randomUUID().toString();
					while (inventory.getAccessor().hasInventoryObject("avatars", lID)) {
						lID = UUID.randomUUID().toString();
					}

					String type = avatar.get("defId").getAsString();
					// Translate defID to a type
					InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
							.getResourceAsStream("defaultitems/avatarhelper.json");
					JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8"))
							.getAsJsonObject().get("Avatars").getAsJsonObject();
					strm.close();

					JsonObject speciesData = helper.get(type).getAsJsonObject();
					for (String species : helper.keySet()) {
						JsonObject ava = helper.get(species).getAsJsonObject();
						if (ava.get("defId").getAsString().equals(type)) {
							type = species;
							break;
						}
					}

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
					components.add("Timestamp", ts);
					components.add("AvatarLook", al);
					components.add("Name", nm);

					// Build data container
					JsonObject lookObj = new JsonObject();
					lookObj.addProperty("defId", speciesData.get("defId").getAsInt());
					lookObj.add("components", components);
					lookObj.addProperty("id", lID);
					lookObj.addProperty("type", 200);

					// Add the look slot
					avatars.add(lookObj);
				}
			} catch (IOException e) {
			}
		}

		// Mark what files to save
		inventory.getAccessor().itemsToSave.add("avatars");
	}

	/**
	 * Unlocks a avatar species
	 * 
	 * @param type Avatar type (either name or defID)
	 */
	public void unlockAvatarSpecies(String type) {
		if (isAvatarSpecieUnlocked(type))
			return;

		try {
			// Translate defID to a type
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/avatarhelper.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Avatars").getAsJsonObject();
			strm.close();

			for (String species : helper.keySet()) {
				JsonObject ava = helper.get(species).getAsJsonObject();
				if (ava.get("defId").getAsString().equals(type)) {
					type = species;
					break;
				}
			}

			// Add the species looks
			JsonObject speciesData = helper.get(type).getAsJsonObject();
			String actorDefID = speciesData.get("info").getAsJsonObject().get("actorClassDefID").getAsString();
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

						// Generate look ID
						String lID = UUID.randomUUID().toString();
						while (inventory.getAccessor().hasInventoryObject("avatars", lID)) {
							lID = UUID.randomUUID().toString();
						}

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
						avatars.add(lookObj);
					}

					// Add slots
					for (int i = cSlots; i < slots; i++) {
						// Generate look ID
						String lID = UUID.randomUUID().toString();
						while (inventory.getAccessor().hasInventoryObject("avatars", lID)) {
							lID = UUID.randomUUID().toString();
						}

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
						avatars.add(lookObj);
					}
				}

				// Mark what files to save
				if (!inventory.getAccessor().itemsToSave.contains("avatars"))
					inventory.getAccessor().itemsToSave.add("avatars");

				// Update the species list
				if (!inventory.containsItem("1"))
					inventory.setItem("1", new JsonArray());
				JsonArray species = inventory.getItem("1").getAsJsonArray();

				// Check if the species is unlocked
				boolean found = false;
				for (JsonElement ele : species) {
					JsonObject sp = ele.getAsJsonObject();
					String spID = sp.get("defID").getAsString();
					if (spID.equals(actorDefID)) {
						found = true;
						break;
					}
				}

				if (!found) {
					JsonObject spD = new JsonObject();

					// Generate item ID
					String sID = UUID.randomUUID().toString();
					while (inventory.getAccessor().hasInventoryObject("1", sID)) {
						sID = UUID.randomUUID().toString();
					}
					// Timestamp
					JsonObject ts = new JsonObject();
					ts.addProperty("ts", System.currentTimeMillis());
					// Build components
					JsonObject components = new JsonObject();
					components.add("Timestamp", ts);
					spD.addProperty("defID", actorDefID);
					spD.add("components", components);
					spD.addProperty("id", sID);
					spD.addProperty("type", 1);
					species.add(spD);

					// Mark what files to save
					if (!inventory.getAccessor().itemsToSave.contains("1"))
						inventory.getAccessor().itemsToSave.add("1");
				}
			}
		} catch (JsonSyntaxException | IOException e) {
		}
	}

	/**
	 * Locks a avatar species
	 * 
	 * @param type Avatar type (either name or defID)
	 */
	public void lockAvatarSpecies(String type) {
		if (!inventory.containsItem("1"))
			return;

		// Load item array
		JsonArray items = inventory.getItem("1").getAsJsonArray();
		String defID = type;

		// Translate type to a defID
		try {
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/avatarhelper.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Avatars").getAsJsonObject();
			strm.close();

			if (helper.has(defID)) {
				defID = helper.get(defID).getAsJsonObject().get("defId").getAsString();
			}
		} catch (JsonSyntaxException | IOException e) {
		}

		// Remove if present
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			if (itm.get("defId").getAsString().equals(defID)) {
				// Remove item
				items.remove(itm);

				// Mark what files to save
				if (!inventory.getAccessor().itemsToSave.contains("1"))
					inventory.getAccessor().itemsToSave.add("1");
				break;
			}
		}
	}

	/**
	 * Checks if a avatar species is unlocked
	 * 
	 * @param type Avatar type (either name or defID)
	 * @return True if unlocked, false otherwise
	 */
	public boolean isAvatarSpecieUnlocked(String type) {
		if (!inventory.containsItem("1"))
			return false;

		String defID = type;

		// Translate type to a defID
		try {
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/avatarhelper.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Avatars").getAsJsonObject();
			strm.close();

			if (helper.has(defID)) {
				defID = helper.get(defID).getAsJsonObject().get("defId").getAsString();
			}
		} catch (JsonSyntaxException | IOException e) {
		}

		// Find species
		if (defID.matches("^[0-9]+$"))
			return inventory.getAccessor().hasInventoryObject(type, Integer.parseInt(defID));
		return false;
	}

	/**
	 * Checks if a avatar part is unlocked
	 * 
	 * @param defID Item ID
	 */
	public boolean isAvatarPartUnlocked(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("2"))
			inventory.setItem("2", new JsonArray());
		JsonArray items = inventory.getItem("2").getAsJsonArray();

		// Find part
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				return true;
			}
		}

		// Part was not found
		return false;
	}

	/**
	 * Unlocks a body mod or wings
	 * 
	 * @param defID Item ID
	 */
	public void unlockAvatarPart(int defID) {
		if (isAvatarPartUnlocked(defID))
			return;

		// Load the inventory object
		if (!inventory.containsItem("2"))
			inventory.setItem("2", new JsonArray());
		JsonArray items = inventory.getItem("2").getAsJsonArray();

		// Generate item ID
		String itmID = UUID.randomUUID().toString();
		while (true) {
			boolean found = false;
			for (JsonElement ele : items) {
				JsonObject itm = ele.getAsJsonObject();
				String itID = itm.get("id").getAsString();
				if (itID.equals(itmID)) {
					found = true;
					break;
				}
			}
			if (!found)
				break;
			itmID = UUID.randomUUID().toString();
		}

		// Add the item
		JsonObject itm = new JsonObject();
		JsonObject ts = new JsonObject();
		ts.addProperty("ts", System.currentTimeMillis());
		// Build components
		JsonObject components = new JsonObject();
		components.add("Timestamp", ts);
		itm.addProperty("defId", defID);
		itm.add("components", components);
		itm.addProperty("id", itmID);
		itm.addProperty("type", 2);
		items.add(itm);

		// Mark what files to save
		if (!inventory.getAccessor().itemsToSave.contains("2"))
			inventory.getAccessor().itemsToSave.add("2");
	}

	/**
	 * Removes a body mod or wings
	 * 
	 * @param defID Item ID
	 */
	public void lockAvatarPart(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("2"))
			inventory.setItem("2", new JsonArray());
		JsonArray items = inventory.getItem("2").getAsJsonArray();

		// Remove if present
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				// Remove item
				items.remove(itm);

				// Mark what files to save
				if (!inventory.getAccessor().itemsToSave.contains("2"))
					inventory.getAccessor().itemsToSave.add("2");
				break;
			}
		}
	}
}
