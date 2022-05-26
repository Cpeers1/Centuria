package org.asf.emuferal.accounts;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public abstract class PlayerInventory {

	private Accessor accessor = new Accessor(this);

	/**
	 * Retrieves the high-level inventory accessor
	 * 
	 * @return Accessor instance
	 */
	public Accessor getAccessor() {
		return accessor;
	}

	public static class Accessor {
		private PlayerInventory inventory;
		private ArrayList<String> itemsToSave = new ArrayList<String>();

		private Accessor(PlayerInventory inventory) {
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
						while (true) {
							boolean found = false;
							for (JsonElement ele : avatars) {
								JsonObject ava = ele.getAsJsonObject();
								String lookID = ava.get("id").getAsString();
								if (lookID.equals(lID)) {
									found = true;
									break;
								}
							}
							if (!found)
								break;

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
					String actorDefID = "0";
					boolean primary = true;
					JsonObject speciesData = helper.get(type).getAsJsonObject();
					for (int i = 0; i < slots + 1; i++) {
						// Generate look ID
						String lID = UUID.randomUUID().toString();
						while (true) {
							boolean found = false;
							for (JsonElement ele : avatars) {
								JsonObject ava = ele.getAsJsonObject();
								String lookID = ava.get("id").getAsString();
								if (lookID.equals(lID)) {
									found = true;
									break;
								}
							}
							if (!found)
								break;

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

						// Scan ID
						actorDefID = speciesData.get("info").getAsJsonObject().get("actorClassDefID").getAsString();

						// Add the avatar
						avatars.add(lookObj);
						primary = false;
					}

					// Add it to the avatar species list
					if (!inventory.containsItem("1"))
						inventory.setItem("1", new JsonArray());
					JsonArray species = inventory.getItem("1").getAsJsonArray();
					JsonObject spD = new JsonObject();
					// Generate item ID
					String sID = UUID.randomUUID().toString();
					while (true) {
						boolean found = false;
						for (JsonElement ele : species) {
							JsonObject sp = ele.getAsJsonObject();
							String spID = sp.get("id").getAsString();
							if (spID.equals(sID)) {
								found = true;
								break;
							}
						}
						if (!found)
							break;
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
					itemsToSave.add("avatars");
					itemsToSave.add("1");
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
			// Load the inventory object
			if (!inventory.containsItem("100"))
				inventory.setItem("100", new JsonArray());
			JsonArray items = inventory.getItem("100").getAsJsonArray();

			// Find object
			for (JsonElement ele : items) {
				JsonObject itm = ele.getAsJsonObject();
				String itID = itm.get("id").getAsString();
				if (itID.equals(id)) {
					// Return clothing object
					return itm;
				}
			}

			return null;
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
			while (true) {
				boolean found = false;
				for (JsonElement ele : items) {
					JsonObject itm = ele.getAsJsonObject();
					String itmID = itm.get("id").getAsString();
					if (itmID.equals(cID)) {
						found = true;
						break;
					}
				}
				if (!found)
					break;

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
		 * Adds a dye to the player's inventory
		 * 
		 * @param defID Dye defID
		 * @return Object UUID
		 */
		public String addDye(int defID) {
			// Find dye
			JsonObject dye = getDyeData(defID);

			// Add one to the quantity field
			int q = dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().get("quantity")
					.getAsInt();
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
			int q = dye.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().get("quantity")
					.getAsInt();
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

	/**
	 * Retrieves a item from the player's inventory
	 * 
	 * @param itemID Inventory item ID
	 * @return JsonElement instance or null
	 */
	public abstract JsonElement getItem(String itemID);

	/**
	 * Saves a item to the player inventory
	 * 
	 * @param itemID   Inventory item ID
	 * @param itemData Item data
	 */
	public abstract void setItem(String itemID, JsonElement itemData);

	/**
	 * Deletes a item from the player inventory
	 * 
	 * @param itemID Inventory item ID
	 */
	public abstract void deleteItem(String itemID);

	/**
	 * Checks if a inventory item is present
	 * 
	 * @param itemID Inventory item ID
	 * @return True if the item is present, false otherwise
	 */
	public abstract boolean containsItem(String itemID);

}
