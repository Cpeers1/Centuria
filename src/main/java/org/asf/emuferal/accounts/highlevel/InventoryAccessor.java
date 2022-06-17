package org.asf.emuferal.accounts.highlevel;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class InventoryAccessor {
	private PlayerInventory inventory;
	private ArrayList<String> itemsToSave = new ArrayList<String>();

	public InventoryAccessor(PlayerInventory inventory) {
		this.inventory = inventory;
	}

	/**
	 * Call this after saving items
	 */
	public void completedSave() {
		itemsToSave.clear();
	}

	/**
	 * Retrieves which items to save
	 * 
	 * @return Array of item IDs to save
	 */
	public String[] getItemsToSave() {
		return itemsToSave.toArray(t -> new String[t]);
	}

	/**
	 * Checks if inventory objects are present
	 * 
	 * @param inventoryId Inventory ID
	 * @param objectId    Object UUID
	 * @return True if present, false otherwise
	 */
	public boolean hasInventoryObject(String inventoryId, String objectId) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			String itID = itm.get("id").getAsString();
			if (itID.equals(objectId)) {
				// Found it
				return true;
			}
		}

		// Could not find it
		return false;
	}

	/**
	 * Retrieves inventory objects by ID
	 * 
	 * @param inventoryId Inventory ID
	 * @param objectId    Object UUID
	 * @return JsonObject instance or null
	 */
	public JsonObject findInventoryObject(String inventoryId, String objectId) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			String itID = itm.get("id").getAsString();
			if (itID.equals(objectId)) {
				// Found it
				return itm;
			}
		}

		// Could not find it
		return null;
	}

	/**
	 * Checks if inventory objects are present
	 * 
	 * @param inventoryId Inventory ID
	 * @param defId       Object DefID
	 * @return True if present, false otherwise
	 */
	public boolean hasInventoryObject(String inventoryId, int defId) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			int itID = itm.get("defId").getAsInt();
			if (itID == defId) {
				// Found it
				return true;
			}
		}

		// Could not find it
		return false;
	}

	/**
	 * Retrieves inventory objects by ID
	 * 
	 * @param inventoryId Inventory ID
	 * @param defId       Object DefID
	 * @return JsonObject instance or null
	 */
	public JsonObject findInventoryObject(String inventoryId, int defId) {
		// Load the inventory object
		if (!inventory.containsItem(inventoryId))
			inventory.setItem(inventoryId, new JsonArray());
		JsonArray items = inventory.getItem(inventoryId).getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			int itID = itm.get("defId").getAsInt();
			if (itID == defId) {
				// Found it
				return itm;
			}
		}

		// Could not find it
		return null;
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
					while (hasInventoryObject("avatars", lID)) {
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
		itemsToSave.add("avatars");
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

			// Unlock the species
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

				// Add the look files and scan in the ID
				boolean primary = true;
				for (int i = 0; i < slots + 1; i++) {
					// Generate look ID
					String lID = UUID.randomUUID().toString();
					while (hasInventoryObject("avatars", lID)) {
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
					avatars.add(lookObj);
					primary = false;
				}

				// Mark what files to save
				if (!itemsToSave.contains("avatars"))
					itemsToSave.add("avatars");

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
					while (hasInventoryObject("1", sID)) {
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
					if (!itemsToSave.contains("1"))
						itemsToSave.add("1");
				}
			}
		} catch (JsonSyntaxException | IOException e) {
		}
	}

	/**
	 * Checks if a avatar species is unlocked
	 * 
	 * @param type Avatar type (either name or defID)
	 * @return True if unlocked, false otherwise
	 */
	public boolean isAvatarSpecieUnlocked(String type) {
		if (!inventory.containsItem("avatars"))
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

		// Find avatar
		JsonArray avatars = inventory.getItem("avatars").getAsJsonArray();
		for (JsonElement ele : avatars) {
			JsonObject ava = ele.getAsJsonObject();
			if (ava.get("defId").getAsString().equals(defID))
				return true;
		}

		// Avatar could not be found
		return false;
	}

	/**
	 * Checks if the player has a specific clothing item
	 * 
	 * @param defID Clothing defID
	 * @return True if the player has the clothing item, false otherwise
	 */
	public boolean hasClothing(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("100"))
			inventory.setItem("100", new JsonArray());
		JsonArray items = inventory.getItem("100").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				return true;
			}
		}

		// Item was not found
		return false;
	}

	/**
	 * Retrieves the amount of a specific clothing item the player has
	 * 
	 * @param defID Clothing defID
	 * @return Amount of the specific item
	 */
	public int getClothingCount(int defID) {
		int count = 0;

		// Load the inventory object
		if (!inventory.containsItem("100"))
			inventory.setItem("100", new JsonArray());
		JsonArray items = inventory.getItem("100").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				count++;
			}
		}

		return count;
	}

	/**
	 * Removes a clothing item
	 * 
	 * @param id Clothing item ID
	 */
	public void removeClothing(String id) {
		// Load the inventory object
		if (!inventory.containsItem("100"))
			inventory.setItem("100", new JsonArray());
		JsonArray items = inventory.getItem("100").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			String itID = itm.get("id").getAsString();
			if (itID.equals(id)) {
				// Remove item
				items.remove(ele);

				// Mark what files to save
				if (!itemsToSave.contains("100"))
					itemsToSave.add("100");

				// End loop
				break;
			}
		}
	}

	/**
	 * Retrieves a clothing inventory object
	 * 
	 * @param id Clothing item ID
	 * @return JsonObject or null
	 */
	public JsonObject getClothingData(String id) {
		return findInventoryObject("100", id);
	}

	/**
	 * Adds a clothing item of a specific defID
	 * 
	 * @param defID         Clothing item defID
	 * @param isInTradeList True to add this item to trade list, false otherwise
	 * @return Item UUID
	 */
	public String addClothing(int defID, boolean isInTradeList) {
		// Load the inventory object
		if (!inventory.containsItem("100"))
			inventory.setItem("100", new JsonArray());
		JsonArray items = inventory.getItem("100").getAsJsonArray();

		// Generate item ID
		String cID = UUID.randomUUID().toString();
		while (hasInventoryObject("100", cID)) {
			cID = UUID.randomUUID().toString();
		}

		// Generate object
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/clothinghelper.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Clothing").getAsJsonObject();
			strm.close();

			// Check existence
			if (helper.has(Integer.toString(defID))) {
				// Create the item
				JsonObject itm = new JsonObject();
				// Timestamp
				JsonObject ts = new JsonObject();
				ts.addProperty("ts", System.currentTimeMillis());
				// Trade thingy
				JsonObject tr = new JsonObject();
				tr.addProperty("isInTradeList", isInTradeList);
				// Build components
				JsonObject components = new JsonObject();
				components.add("Tradable", tr);
				components.add("Colorable", helper.get(Integer.toString(defID)));
				components.add("Timestamp", ts);
				itm.addProperty("defId", defID);
				itm.add("components", components);
				itm.addProperty("id", cID);
				itm.addProperty("type", 100);

				// Add it
				items.add(itm);

				// Mark what files to save
				if (!itemsToSave.contains("100"))
					itemsToSave.add("100");
			}
		} catch (IOException e) {
		}

		// Return ID
		return cID;
	}

	/**
	 * Retrieves the default color of a clothing color channel
	 * 
	 * @param defID   Clothing defID
	 * @param channel Channel number
	 * @return HSV string or null
	 */
	public String getDefaultClothingChannelHSV(int defID, int channel) {
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/clothinghelper.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Clothing").getAsJsonObject();
			strm.close();

			// Check existence
			if (helper.has(Integer.toString(defID))) {
				// Find channel
				JsonObject data = helper.get(Integer.toString(defID)).getAsJsonObject();
				if (data.has("color" + channel + "HSV"))
					return data.get("color" + channel + "HSV").getAsJsonObject().get("_hsv").getAsString();
			}
		} catch (IOException e) {
		}
		return null;
	}

	/**
	 * Checks if the player has a specific furniture item
	 * 
	 * @param defID Furniture defID
	 * @return True if the player has the furniture item, false otherwise
	 */
	public boolean hasFurniture(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("102"))
			inventory.setItem("102", new JsonArray());
		JsonArray items = inventory.getItem("102").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				return true;
			}
		}

		// Item was not found
		return false;
	}

	/**
	 * Retrieves the amount of a specific furniture item the player has
	 * 
	 * @param defID Furniture defID
	 * @return Amount of the specific item
	 */
	public int getFurnitureCount(int defID) {
		int count = 0;

		// Load the inventory object
		if (!inventory.containsItem("102"))
			inventory.setItem("102", new JsonArray());
		JsonArray items = inventory.getItem("102").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				count++;
			}
		}

		return count;
	}

	/**
	 * Removes a furniture item
	 * 
	 * @param id Furniture item ID
	 */
	public void removeFurniture(String id) {
		// Load the inventory object
		if (!inventory.containsItem("102"))
			inventory.setItem("102", new JsonArray());
		JsonArray items = inventory.getItem("102").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			String itID = itm.get("id").getAsString();
			if (itID.equals(id)) {
				// Remove item
				items.remove(ele);

				// Mark what files to save
				if (!itemsToSave.contains("102"))
					itemsToSave.add("102");

				// End loop
				break;
			}
		}
	}

	/**
	 * Retrieves a furniture inventory object
	 * 
	 * @param id Furniture item ID
	 * @return JsonObject or null
	 */
	public JsonObject getFurnitureData(String id) {
		return findInventoryObject("102", id);
	}

	/**
	 * Adds a furniture item of a specific defID
	 * 
	 * @param defID         Furniture item defID
	 * @param isInTradeList True to add this item to trade list, false otherwise
	 * @return Item UUID
	 */
	public String addFurniture(int defID, boolean isInTradeList) {
		// Load the inventory object
		if (!inventory.containsItem("102"))
			inventory.setItem("102", new JsonArray());
		JsonArray items = inventory.getItem("102").getAsJsonArray();

		// Generate item ID
		String cID = UUID.randomUUID().toString();
		while (hasInventoryObject("102", cID)) {
			cID = UUID.randomUUID().toString();
		}

		// Generate object
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/furniturehelper.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Furniture").getAsJsonObject();
			strm.close();

			// Check existence
			if (helper.has(Integer.toString(defID))) {
				// Create the item
				JsonObject itm = new JsonObject();
				// Timestamp
				JsonObject ts = new JsonObject();
				ts.addProperty("ts", System.currentTimeMillis());
				// Trade thingy
				JsonObject tr = new JsonObject();
				tr.addProperty("isInTradeList", isInTradeList);
				// Build components
				JsonObject components = new JsonObject();
				components.add("Tradable", tr);
				components.add("Colorable", helper.get(Integer.toString(defID)));
				components.add("Placeable", new JsonObject());
				components.add("Timestamp", ts);
				itm.addProperty("defId", defID);
				itm.add("components", components);
				itm.addProperty("id", cID);
				itm.addProperty("type", 102);

				// Add it
				items.add(itm);

				// Mark what files to save
				if (!itemsToSave.contains("102"))
					itemsToSave.add("102");
			}
		} catch (IOException e) {
		}

		// Return ID
		return cID;
	}

	/**
	 * Retrieves the default color of a furniture color channel
	 * 
	 * @param defID   Furniture defID
	 * @param channel Channel number
	 * @return HSV string or null
	 */
	public String getDefaultFurnitureChannelHSV(int defID, int channel) {
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/furniturehelper.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Furniture").getAsJsonObject();
			strm.close();

			// Check existence
			if (helper.has(Integer.toString(defID))) {
				// Find channel
				JsonObject data = helper.get(Integer.toString(defID)).getAsJsonObject();
				if (data.has("color" + channel + "HSV"))
					return data.get("color" + channel + "HSV").getAsJsonObject().get("_hsv").getAsString();
			}
		} catch (IOException e) {
		}
		return null;
	}

	/**
	 * Adds a dye to the player's inventory
	 * 
	 * @param defID Dye defID
	 * @return Object UUID
	 */
	public String addDye(int defID) {
		// Find dye
		JsonObject dye = getDyeData(defID);

		// Add one to the quantity field
		int q = dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().get("quantity").getAsInt();
		dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().remove("quantity");
		dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().addProperty("quantity", q + 1);

		// Mark what files to save
		if (!itemsToSave.contains("111"))
			itemsToSave.add("111");

		// Return ID
		return dye.get("id").getAsString();
	}

	/**
	 * Removes a dye to the player's inventory
	 * 
	 * @param defID Dye defID
	 */
	public void removeDye(int defID) {
		// Find dye
		JsonObject dye = getDyeData(defID);

		// Remove one to the quantity field
		int q = dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().get("quantity").getAsInt();
		dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().remove("quantity");
		dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().addProperty("quantity", q - 1);

		if (q - 1 <= 0) {
			// Remove object
			inventory.getItem("111").getAsJsonArray().remove(dye);
		}

		// Mark what files to save
		if (!itemsToSave.contains("111"))
			itemsToSave.add("111");
	}

	/**
	 * Retrieves a dye inventory object
	 * 
	 * @param id Dye item ID
	 * @return JsonObject or null
	 */
	public JsonObject getDyeData(String id) {
		// Load the inventory object
		if (!inventory.containsItem("111"))
			inventory.setItem("111", new JsonArray());
		JsonArray items = inventory.getItem("111").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			String itID = itm.get("id").getAsString();
			if (itID.equals(id)) {
				// Return dye
				return itm;
			}
		}

		return null;
	}

	/**
	 * Retrieves the HSV value of a dye
	 * 
	 * @param defID Dye defID
	 * @return HSV value or null
	 */
	public String getDyeHSV(int defID) {
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/dyehelper.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Dyes").getAsJsonObject();
			strm.close();

			if (helper.has(Integer.toString(defID)))
				return helper.get(Integer.toString(defID)).getAsString();
		} catch (IOException e) {
		}

		return null;
	}

	/**
	 * Removes a dye to the player's inventory
	 * 
	 * @param id Dye item ID
	 */
	public void removeDye(String id) {
		// Load the inventory object
		if (!inventory.containsItem("111"))
			inventory.setItem("111", new JsonArray());
		JsonArray items = inventory.getItem("111").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject dye = ele.getAsJsonObject();
			String itID = dye.get("id").getAsString();
			if (itID.equals(id)) {
				// Remove one to the quantity field
				int q = dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().get("quantity")
						.getAsInt();
				dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().remove("quantity");
				dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().addProperty("quantity",
						q - 1);

				if (q - 1 <= 0) {
					// Remove object
					inventory.getItem("111").getAsJsonArray().remove(dye);
				}

				// Mark what files to save
				if (!itemsToSave.contains("111"))
					itemsToSave.add("111");

				// End loop
				break;
			}
		}
	}

	/**
	 * Checks if the player has a specific dye
	 * 
	 * @param defID Dye defID
	 * @return True if the player has the dye, false otherwise
	 */
	public boolean hasDye(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("111"))
			inventory.setItem("111", new JsonArray());
		JsonArray items = inventory.getItem("111").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				return true;
			}
		}

		// Item was not found
		return false;
	}

	// Retrieves information objects for dyes and makes it if not present
	private JsonObject getDyeData(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("111"))
			inventory.setItem("111", new JsonArray());
		JsonArray items = inventory.getItem("111").getAsJsonArray();

		// Find object
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				return ele.getAsJsonObject();
			}
		}

		// Add the item
		JsonObject itm = new JsonObject();
		// Timestamp
		JsonObject ts = new JsonObject();
		ts.addProperty("ts", System.currentTimeMillis());
		// Trade thingy
		JsonObject tr = new JsonObject();
		tr.addProperty("isInTradeList", false);
		// Quantity
		JsonObject qt = new JsonObject();
		qt.addProperty("quantity", 0);
		// Build components
		JsonObject components = new JsonObject();
		components.add("Tradable", tr);
		components.add("Quantity", qt);
		components.add("Timestamp", ts);
		itm.addProperty("defId", defID);
		itm.add("components", components);
		itm.addProperty("id", UUID.nameUUIDFromBytes(Integer.toString(defID).getBytes()).toString());
		itm.addProperty("type", 111);

		// Add it
		items.add(itm);

		// Mark what files to save
		if (!itemsToSave.contains("111"))
			itemsToSave.add("111");

		return itm;
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
		if (!itemsToSave.contains("2"))
			itemsToSave.add("2");
	}

	/**
	 * Finds all unlocked island types
	 * 
	 * @return Array of island type defIDs
	 */
	public int[] getUnlockedIslandTypes() {
		ArrayList<Integer> types = new ArrayList<Integer>();

		// Load the inventory object
		if (!inventory.containsItem("6"))
			inventory.setItem("6", new JsonArray());
		JsonArray items = inventory.getItem("6").getAsJsonArray();

		// Find all IDs
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();

			// Add ID
			int itID = itm.get("defId").getAsInt();
			if (!types.contains(itID))
				types.add(itID);
		}

		// Return the type IDs
		int[] typeIds = new int[types.size()];
		for (int i = 0; i < typeIds.length; i++)
			typeIds[i] = types.get(i);
		return typeIds;
	}

	/**
	 * Retrieves the count of items of a specific island type
	 * 
	 * @param defID Island type ID
	 * @return Item count of the given island type
	 */
	public int getIslandTypeItemCount(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("6"))
			inventory.setItem("6", new JsonArray());
		JsonArray items = inventory.getItem("6").getAsJsonArray();
		int count = 0;

		// Find island
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();

			// Check ID
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				count++;
			}
		}

		// Not found
		return count;
	}

	/**
	 * Creates a new save for a specific island type and saves it to the inventory
	 * 
	 * @param defID Island type ID
	 * @return Item ID string
	 */
	public String addIslandToInventory(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("6"))
			inventory.setItem("6", new JsonArray());
		JsonArray items = inventory.getItem("6").getAsJsonArray();

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

		// Create the object
		JsonObject itm = new JsonObject();
		JsonObject ts = new JsonObject();
		ts.addProperty("ts", System.currentTimeMillis());
		// Build island info object
		JsonObject island = new JsonObject();
		island.addProperty("gridId", 0);
		island.addProperty("themeDefId", 0);
		JsonObject components = new JsonObject();
		components.add("Island", island);
		components.add("Timestamp", ts);
		itm.addProperty("defId", defID);
		itm.add("components", components);
		itm.addProperty("id", itmID);
		itm.addProperty("type", 6);
		items.add(itm);

		// Mark what files to save
		if (!itemsToSave.contains("6"))
			itemsToSave.add("6");

		// Return ID
		return itmID;
	}

	/**
	 * Finds a island info object
	 * 
	 * @param id Island info object ID
	 * @return JSON object or null
	 */
	public JsonObject getIslandTypeObject(String id) {
		return findInventoryObject("6", id);
	}

	/**
	 * Checks if a island type is unlocked or not
	 * 
	 * @param defID Island type ID
	 * @return True if unlocked, false otherwise
	 */
	public boolean isIslandTypeUnlocked(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("6"))
			inventory.setItem("6", new JsonArray());
		JsonArray items = inventory.getItem("6").getAsJsonArray();

		// Find island
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();

			// Check ID
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				return true;
			}
		}

		// Not found
		return false;
	}

	/**
	 * Finds all unlocked house types
	 * 
	 * @return Array of house type defIDs
	 */
	public int[] getUnlockedHouseTypes() {
		ArrayList<Integer> types = new ArrayList<Integer>();

		// Load the inventory object
		if (!inventory.containsItem("5"))
			inventory.setItem("5", new JsonArray());
		JsonArray items = inventory.getItem("5").getAsJsonArray();

		// Find all IDs
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();

			// Add ID
			int itID = itm.get("defId").getAsInt();
			if (!types.contains(itID))
				types.add(itID);
		}

		// Return the type IDs
		int[] typeIds = new int[types.size()];
		for (int i = 0; i < typeIds.length; i++)
			typeIds[i] = types.get(i);
		return typeIds;
	}

	/**
	 * Retrieves the count of items of a specific house type
	 * 
	 * @param defID House type ID
	 * @return Item count of the given house type
	 */
	public int getHouseTypeItemCount(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("5"))
			inventory.setItem("5", new JsonArray());
		JsonArray items = inventory.getItem("5").getAsJsonArray();
		int count = 0;

		// Find house
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();

			// Check ID
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				count++;
			}
		}

		// Not found
		return count;
	}

	/**
	 * Creates a new save for a specific house type and saves it to the inventory
	 * 
	 * @param defID House type ID
	 * @return Item ID string
	 */
	public String addHouseToInventory(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("5"))
			inventory.setItem("5", new JsonArray());
		JsonArray items = inventory.getItem("5").getAsJsonArray();

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

		// Create the object
		JsonObject itm = new JsonObject();
		JsonObject ts = new JsonObject();
		ts.addProperty("ts", System.currentTimeMillis());
		// Build house info object
		JsonObject house = new JsonObject();
		house.addProperty("stage", 0);
		house.add("roomData", new JsonArray());
		house.addProperty("x", 0);
		house.addProperty("y", 0);
		house.addProperty("gridId", 0);
		house.addProperty("themeDefId", 0);
		JsonArray enlargedAreas = new JsonArray();
		for (int i = 0; i < 10; i++) {
			enlargedAreas.add(0);
		}
		house.add("enlargedAreas", enlargedAreas);
		JsonObject components = new JsonObject();
		components.add("House", house);
		components.add("Timestamp", ts);
		itm.addProperty("defId", defID);
		itm.add("components", components);
		itm.addProperty("id", itmID);
		itm.addProperty("type", 5);
		items.add(itm);

		// Mark what files to save
		if (!itemsToSave.contains("5"))
			itemsToSave.add("5");

		// Return ID
		return itmID;
	}

	/**
	 * Finds a house info object
	 * 
	 * @param id House info object ID
	 * @return JSON object or null
	 */
	public JsonObject getHouseTypeObject(String id) {
		return findInventoryObject("5", id);
	}

	/**
	 * Checks if a house type is unlocked or not
	 * 
	 * @param defID House type ID
	 * @return True if unlocked, false otherwise
	 */
	public boolean isHouseTypeUnlocked(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("5"))
			inventory.setItem("5", new JsonArray());
		JsonArray items = inventory.getItem("5").getAsJsonArray();

		// Find house
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();

			// Check ID
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				return true;
			}
		}

		// Not found
		return false;
	}

	/**
	 * Finds all unlocked sanctuary classes
	 * 
	 * @return Array of sanctuary class defIDs
	 */
	public int[] getUnlockedSanctuaryClasses() {
		ArrayList<Integer> types = new ArrayList<Integer>();

		// Load the inventory object
		if (!inventory.containsItem("10"))
			inventory.setItem("10", new JsonArray());
		JsonArray items = inventory.getItem("10").getAsJsonArray();

		// Find all IDs
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();

			// Add ID
			int itID = itm.get("defId").getAsInt();
			if (!types.contains(itID))
				types.add(itID);
		}

		// Return the type IDs
		int[] typeIds = new int[types.size()];
		for (int i = 0; i < typeIds.length; i++)
			typeIds[i] = types.get(i);
		return typeIds;
	}

	/**
	 * Creates a new save for a specific sanctuary class and saves it to the
	 * inventory
	 * 
	 * @param defID Sanctuary class ID
	 * @return Item ID string
	 */
	public String addSanctuaryClassToInventory(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("10"))
			inventory.setItem("10", new JsonArray());
		JsonArray items = inventory.getItem("10").getAsJsonArray();

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

		// Create the object
		JsonObject itm = new JsonObject();
		JsonObject ts = new JsonObject();
		ts.addProperty("ts", System.currentTimeMillis());
		// Build class info object
		JsonObject classI = new JsonObject();
		classI.addProperty("stage", 0);
		JsonArray enlargedAreas = new JsonArray();
		for (int i = 0; i < 10; i++) {
			enlargedAreas.add(0);
		}
		classI.add("enlargedAreas", enlargedAreas);
		JsonObject components = new JsonObject();
		components.add("SanctuaryClass", classI);
		components.add("Timestamp", ts);
		itm.addProperty("defId", defID);
		itm.add("components", components);
		itm.addProperty("id", itmID);
		itm.addProperty("type", 10);
		items.add(itm);

		// Mark what files to save
		if (!itemsToSave.contains("10"))
			itemsToSave.add("10");

		// Return ID
		return itmID;
	}

	/**
	 * Finds a sanctuary class object
	 * 
	 * @param id Sanctuary class object ID
	 * @return JSON object or null
	 */
	public JsonObject getSanctuaryClassObject(String id) {
		return findInventoryObject("10", id);
	}

	/**
	 * Checks if a sanctuary class is unlocked or not
	 * 
	 * @param defID Sanctuary class ID
	 * @return True if unlocked, false otherwise
	 */
	public boolean isSanctuaryClassUnlocked(int defID) {
		// Load the inventory object
		if (!inventory.containsItem("10"))
			inventory.setItem("10", new JsonArray());
		JsonArray items = inventory.getItem("10").getAsJsonArray();

		// Find class
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();

			// Check ID
			int itID = itm.get("defId").getAsInt();
			if (itID == defID) {
				return true;
			}
		}

		// Not found
		return false;
	}

	/**
	 * Retrieves the sanctuary look count
	 * 
	 * @return The amount of sanctuary looks a player has
	 */
	public int getSanctuaryLookCount() {
		// Load the inventory object
		if (!inventory.containsItem("201"))
			return 0;
		return inventory.getItem("201").getAsJsonArray().size();
	}

	/**
	 * Finds a sanctuary look
	 * 
	 * @param lookID Sanctuary look ID
	 * @return Sanctuary JSON object or null
	 */
	public JsonObject getSanctuaryLook(String lookID) {
		return findInventoryObject("201", lookID);
	}

	/**
	 * Retrieves the first sanctuary look found in the inventory
	 * 
	 * @return Sanctuary JSON object or null
	 */
	public JsonObject getFirstSanctuaryLook() {
		// Load the inventory object
		if (!inventory.containsItem("201"))
			inventory.setItem("201", new JsonArray());
		JsonArray items = inventory.getItem("201").getAsJsonArray();

		// Find sanctuary
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			return itm;
		}

		// Could not find one
		return null;
	}

	/**
	 * Retrieves an array of sanctuary look IDs
	 * 
	 * @return Array of sanctuary look IDs
	 */
	public String[] getSanctuaryLookIDs() {
		// Load the inventory object
		if (!inventory.containsItem("201"))
			inventory.setItem("201", new JsonArray());
		JsonArray items = inventory.getItem("201").getAsJsonArray();

		// Build list
		ArrayList<String> sancs = new ArrayList<String>();

		// Find sanctuaries
		for (JsonElement ele : items) {
			JsonObject itm = ele.getAsJsonObject();
			sancs.add(itm.get("id").getAsString());
		}

		// Could not find one
		return sancs.toArray(t -> new String[t]);
	}

	/**
	 * Adds an extra sanctuary look slot to the player inventory
	 */
	public void addExtraSanctuarySlot() {
		// Load the inventory object
		if (!inventory.containsItem("201"))
			inventory.setItem("201", new JsonArray());
		JsonArray items = inventory.getItem("201").getAsJsonArray();

		// Add for each sanctuary class
		for (int classId : getUnlockedSanctuaryClasses()) {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("defaultitems/sanctuaryclasseshelper.json");
			JsonObject helper;
			try {
				helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
						.get("SanctuaryClasses").getAsJsonObject();
				strm.close();
			} catch (JsonSyntaxException | IOException e) {
				throw new RuntimeException(e);
			}

			// Load ids
			JsonObject classData = helper.get(Integer.toString(classId)).getAsJsonObject();
			int islandId = classData.get("islandDefId").getAsInt();
			int houseId = classData.get("houseDefId").getAsInt();
			int lookDefId = classData.get("lookDefId").getAsInt();

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

			// Build object
			JsonObject itm = new JsonObject();
			JsonObject ts = new JsonObject();
			ts.addProperty("ts", System.currentTimeMillis());
			// Build sanctuary info object
			JsonObject sanctuary = new JsonObject();
			JsonObject sanctuaryInfo = new JsonObject();
			// Create the house
			String id = addHouseToInventory(houseId);
			sanctuaryInfo.addProperty("houseDefId", houseId);
			sanctuaryInfo.addProperty("houseInvId", id);
			sanctuaryInfo.add("placementInfo", new JsonObject());
			// Create the island
			id = addIslandToInventory(islandId);
			sanctuaryInfo.addProperty("islandDefId", islandId);
			sanctuaryInfo.addProperty("islandInvId", id);
			// Create class
			id = addSanctuaryClassToInventory(classId);
			sanctuaryInfo.addProperty("classInvId", id);
			sanctuary.add("info", sanctuaryInfo);
			// Build components
			JsonObject components = new JsonObject();
			components.add("SanctuaryLook", sanctuary);
			components.add("Timestamp", ts);
			itm.addProperty("defId", lookDefId);
			itm.add("components", components);
			itm.addProperty("id", itmID);
			itm.addProperty("type", 201);
			items.add(itm);

			// Mark what files to save
			if (!itemsToSave.contains("201"))
				itemsToSave.add("201");
		}
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
				if (!itemsToSave.contains("2"))
					itemsToSave.add("2");
				break;
			}
		}
	}
}
