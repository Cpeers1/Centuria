package org.asf.emuferal.accounts.highlevel;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.InventoryDefinitionContainer;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl.AvatarSpeciesHelper;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl.BodyModHelper;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl.ClothingHelper;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl.CurrencyHelper;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl.FurnitureHelper;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl.GenericHelper;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl.GenericHelperWithQuantity;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl.SanctuaryClassHelper;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl.SanctuaryHouseHelper;
import org.asf.emuferal.accounts.highlevel.itemdata.inventory.impl.SanctuaryIslandHelper;
import org.asf.emuferal.accounts.highlevel.itemdata.item.ItemComponent;
import org.asf.emuferal.accounts.highlevel.itemdata.item.ItemInfo;
import org.asf.emuferal.enums.inventory.InventoryStorageType;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemRemovedPacket;
import org.asf.emuferal.players.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ItemAccessor {

	private Player player;
	private PlayerInventory inventory;
	private static HashMap<String, ItemInfo> definitions = new HashMap<String, ItemInfo>();

	private static ItemComponent tradelisComponent() {
		JsonObject tr = new JsonObject();
		tr.addProperty("isInTradeList", false);

		return new ItemComponent("Tradable", tr);
	}

	private static ItemComponent enigmaComonent() {
		JsonObject e = new JsonObject();
		e.addProperty("activated", false);

		return new ItemComponent("Enigma", e);
	}

	@SuppressWarnings("serial")
	private static final Map<String, InventoryDefinitionContainer> inventoryTypeMap = new HashMap<String, InventoryDefinitionContainer>() {
		{
			// Avatar species
			put("1", new InventoryDefinitionContainer(InventoryStorageType.SINGLE_ITEM, new AvatarSpeciesHelper()));

			// Body mods
			put("2", new InventoryDefinitionContainer(InventoryStorageType.SINGLE_ITEM, new BodyModHelper()));

			// Clothing
			put("100", new InventoryDefinitionContainer(InventoryStorageType.OBJECT_BASED, new ClothingHelper()));

			// Sanctuary classes
			put("10", new InventoryDefinitionContainer(InventoryStorageType.SINGLE_ITEM, new SanctuaryClassHelper()));

			// Sanctuary houses
			put("5", new InventoryDefinitionContainer(InventoryStorageType.OBJECT_BASED, new SanctuaryHouseHelper()));

			// Sanctuary islands
			put("6", new InventoryDefinitionContainer(InventoryStorageType.OBJECT_BASED, new SanctuaryIslandHelper()));

			// Furniture
			put("102", new InventoryDefinitionContainer(InventoryStorageType.OBJECT_BASED, new FurnitureHelper()));

			// Currency
			put("104", new InventoryDefinitionContainer(InventoryStorageType.QUANTITY_BASED, new CurrencyHelper()));

			// Resources
			put("103", new InventoryDefinitionContainer(InventoryStorageType.QUANTITY_BASED,
					new GenericHelperWithQuantity("103", tradelisComponent())));

			// Dyes
			put("111", new InventoryDefinitionContainer(InventoryStorageType.QUANTITY_BASED,
					new GenericHelperWithQuantity("111", tradelisComponent())));

			// Enigmas (ty to Mewt/Uzukara for pointing this out)
			put("7", new InventoryDefinitionContainer(InventoryStorageType.OBJECT_BASED,
					new GenericHelper("7", enigmaComonent())));

			// Inspirations
			put("8", new InventoryDefinitionContainer(InventoryStorageType.SINGLE_ITEM,
					new GenericHelper("8", new ItemComponent("Inspiration", new JsonObject()))));
		}
	};

	static {
		try {
			// Load the helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader()
					.getResourceAsStream("itemdefinitions.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Definitions").getAsJsonObject();
			strm.close();

			// Load all items
			helper.keySet().forEach(itemID -> {
				JsonObject itmI = helper.get(itemID).getAsJsonObject();
				ItemInfo info = new ItemInfo();
				info.inventory = itmI.get("inventory").getAsString();
				info.objectName = itmI.get("objectName").getAsString();
				if (!info.inventory.equals("0"))
					definitions.put(itemID, info);
			});
		} catch (IOException e) {
		}
	}

	public ItemAccessor(PlayerInventory inventory, Player player) {
		this.inventory = inventory;
		this.player = player;
	}

	/**
	 * Retrieves an array of defIDs for a specific inventory type
	 * 
	 * @param inventory Inventory type string
	 * @return Array of defIDs
	 */
	public static String[] getItemDefinitionsIn(String inventory) {
		return definitions.keySet().stream().filter(t -> definitions.get(t).inventory.equals(inventory))
				.toArray(t -> new String[t]);
	}

	/**
	 * Retrieves the inventory type of a item by defID
	 * 
	 * @param defID Item defID
	 * @return Inventory type string or null
	 */
	public static String getInventoryTypeOf(int defID) {
		// Find definition
		ItemInfo info = definitions.get(Integer.toString(defID));
		if (info == null)
			return null;
		return info.inventory;
	}

	/**
	 * Adds items by object
	 * 
	 * @param object Inventory item object
	 * @return New item ID string or null if invalid
	 */
	public String add(JsonObject object) {
		JsonObject obj = null;

		// Find definition
		ItemInfo info = definitions.get(Integer.toString(object.get("defId").getAsInt()));
		if (info == null)
			return null;

		// Find inventory
		if (!inventoryTypeMap.containsKey(info.inventory))
			return null;
		InventoryDefinitionContainer container = inventoryTypeMap.get(info.inventory);

		// Find type handler
		switch (container.inventoryType) {

		// Single-item
		case SINGLE_ITEM: {
			// Check if the item is present
			if (inventory.getAccessor().hasInventoryObject(info.inventory, object.get("defId").getAsInt()))
				return null; // invalid

			// Add item directly
			obj = container.inventoryInteraction.addOne(inventory, object);
			break;
		}

		// Object-based item
		case OBJECT_BASED: {
			// Add item directly
			obj = container.inventoryInteraction.addOne(inventory, object);
			break;
		}

		// Quantity-based item
		case QUANTITY_BASED: {
			// Check quantity field
			if (object.has("components") && object.get("components").getAsJsonObject().has("Quantity")) {
				int q = object.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().get("quantity")
						.getAsInt();

				// Add items
				if (q >= 0) {
					if (!inventory.getAccessor().hasInventoryObject(info.inventory, object.get("defId").getAsInt())) {
						// Add item directly
						obj = container.inventoryInteraction.addOne(inventory, object);
					} else {
						// Add to existing
						container.inventoryInteraction.addMultiple(inventory, object.get("defId").getAsInt(), q);
						obj = inventory.getAccessor().findInventoryObject(info.inventory,
								object.get("defId").getAsInt());
					}
				}
			} else {
				// Add item directly
				obj = container.inventoryInteraction.addOne(inventory, object);
			}
			break;
		}

		}

		if (obj != null && player != null) {
			// Send packet if successful
			InventoryItemPacket pk = new InventoryItemPacket();
			JsonArray arr = new JsonArray();
			arr.add(obj);
			pk.item = arr;
			player.client.sendPacket(pk);

			// Save items
			for (String itm : inventory.getAccessor().itemsToSave) {
				inventory.setItem(itm, inventory.getItem(itm));

				if (!info.inventory.equals(itm)) {
					// Sync unsaved inventories
					pk = new InventoryItemPacket();
					pk.item = inventory.getItem(itm);
					player.client.sendPacket(pk);
				}
			}
			inventory.getAccessor().completedSave();
		}

		// Return id
		return (obj != null ? obj.get("id").getAsString() : null);
	}

	/**
	 * Adds items by DefID
	 * 
	 * @param defID Item defID
	 * @return Item ID string or null if invalid
	 */
	public String add(int defID) {
		JsonObject obj = null;

		// Find definition
		ItemInfo info = definitions.get(Integer.toString(defID));
		if (info == null)
			return null;

		// Find inventory
		if (!inventoryTypeMap.containsKey(info.inventory))
			return null;
		InventoryDefinitionContainer container = inventoryTypeMap.get(info.inventory);

		// Find type handler
		switch (container.inventoryType) {

		// Single-item
		case SINGLE_ITEM: {
			// Check if the item is present
			if (inventory.getAccessor().hasInventoryObject(info.inventory, defID))
				return null; // invalid

			// Fall through to the quantity-based handler as the rest is the same
		}

		// Object-based and quantity-based items
		case OBJECT_BASED:
		case QUANTITY_BASED: {
			// Add item
			obj = container.inventoryInteraction.addOne(inventory, defID);
			break;
		}

		}

		if (obj != null && player != null) {
			// Send packet if successful
			InventoryItemPacket pk = new InventoryItemPacket();
			JsonArray arr = new JsonArray();
			arr.add(obj);
			pk.item = arr;
			player.client.sendPacket(pk);

			// Save items
			for (String itm : inventory.getAccessor().itemsToSave) {
				inventory.setItem(itm, inventory.getItem(itm));

				if (!info.inventory.equals(itm)) {
					// Sync unsaved inventories
					pk = new InventoryItemPacket();
					pk.item = inventory.getItem(itm);
					player.client.sendPacket(pk);
				}
			}
			inventory.getAccessor().completedSave();
		}

		// Return id
		return (obj != null ? obj.get("id").getAsString() : null);
	}

	/**
	 * Adds items by DefID
	 * 
	 * @param defID Item defID
	 * @param count Amount of the item to add
	 * @return Array of item IDs that were added
	 */
	public String[] add(int defID, int count) {
		// Check validity
		if (count <= 0)
			return new String[0]; // Nonsense count so lets return an empty array

		JsonObject[] objs = null;

		// Find definition
		ItemInfo info = definitions.get(Integer.toString(defID));
		if (info == null)
			return new String[0];

		// Find inventory
		if (!inventoryTypeMap.containsKey(info.inventory))
			return new String[0];
		InventoryDefinitionContainer container = inventoryTypeMap.get(info.inventory);

		// Find type handler
		switch (container.inventoryType) {

		// Single-item
		case SINGLE_ITEM: {
			// Check if the item is present
			if (count > 1 || inventory.getAccessor().hasInventoryObject(info.inventory, defID))
				return new String[0]; // invalid

			// Fall through to the quantity-based handler as the rest is the same
		}

		// Object-based and quantity-based items
		case OBJECT_BASED:
		case QUANTITY_BASED: {
			// Add item
			objs = container.inventoryInteraction.addMultiple(inventory, defID, count);
			break;
		}

		}

		if (objs != null && player != null) {
			// Send packet if successful
			ArrayList<String> syncedInventories = new ArrayList<String>();
			InventoryItemPacket pk = new InventoryItemPacket();
			JsonArray arr = new JsonArray();
			for (JsonObject obj : objs) {
				arr.add(obj);

				if (!syncedInventories.contains(obj.get("type").getAsString())) {
					syncedInventories.add(obj.get("type").getAsString());
				}
			}
			pk.item = arr;
			player.client.sendPacket(pk);

			// Save items
			for (String itm : inventory.getAccessor().itemsToSave) {
				inventory.setItem(itm, inventory.getItem(itm));

				if (!syncedInventories.contains(itm)) {
					// Sync unsaved inventories
					pk = new InventoryItemPacket();
					pk.item = inventory.getItem(itm);
					player.client.sendPacket(pk);
				}
			}
			inventory.getAccessor().completedSave();
		}

		// Return ids
		return (objs != null ? Stream.of(objs).map(t -> t.get("id").getAsString()).toArray(t -> new String[t])
				: new String[0]);
	}

	/**
	 * Removes items by DefID
	 * 
	 * @param defID Item defID
	 * @return True if successful, false otherwise
	 */
	public boolean remove(int defID) {
		// Find definition
		ItemInfo info = definitions.get(Integer.toString(defID));
		if (info == null)
			return false;

		// Find inventory
		if (!inventoryTypeMap.containsKey(info.inventory))
			return false;
		InventoryDefinitionContainer container = inventoryTypeMap.get(info.inventory);

		String removedItemUUID = null;

		// Find type handler
		switch (container.inventoryType) {

		// Single-item
		case SINGLE_ITEM: {
			// Check if the item is present
			if (!inventory.getAccessor().hasInventoryObject(info.inventory, defID))
				return false; // invalid

			// Fall through to the quantity-based handler as the rest is the same
		}

		// Object-based and quantity-based items
		case OBJECT_BASED:
		case QUANTITY_BASED: {
			// Remove item
			removedItemUUID = container.inventoryInteraction.removeOne(inventory, defID);
			if (removedItemUUID == null)
				return false;
			break;
		}

		}

		if (player != null) {
			// Send packet if successful
			if (inventory.getAccessor().hasInventoryObject(info.inventory, defID)) {
				JsonArray arr = new JsonArray();
				arr.add(inventory.getAccessor().findInventoryObject(info.inventory, defID));
				InventoryItemPacket pk = new InventoryItemPacket();
				pk.item = arr;
				player.client.sendPacket(pk);
			} else {
				InventoryItemRemovedPacket pk = new InventoryItemRemovedPacket();
				pk.items = new String[] { removedItemUUID };
				player.client.sendPacket(pk);
			}

			// Save items
			for (String itm : inventory.getAccessor().itemsToSave) {
				inventory.setItem(itm, inventory.getItem(itm));

				if (!info.inventory.equals(itm)) {
					// Sync unsaved inventories
					InventoryItemPacket pk = new InventoryItemPacket();
					pk.item = inventory.getItem(itm);
					player.client.sendPacket(pk);
				}
			}
			inventory.getAccessor().completedSave();
		}

		// Return success
		return true;
	}

	/**
	 * Remove items by DefID
	 * 
	 * @param defID Item defID
	 * @param count Amount of the item to remove
	 * @return True if successful, false otherwise
	 */
	public boolean remove(int defID, int count) {
		// Check validity
		if (count <= 0)
			return false;

		// Find definition
		ItemInfo info = definitions.get(Integer.toString(defID));
		if (info == null)
			return false;

		// Find inventory
		if (!inventoryTypeMap.containsKey(info.inventory))
			return false;
		InventoryDefinitionContainer container = inventoryTypeMap.get(info.inventory);

		String[] removedItemUUIDs = null;

		// Find type handler
		switch (container.inventoryType) {

		// Single-item
		case SINGLE_ITEM: {
			// Check if the item is present
			if (count > 1 || !inventory.getAccessor().hasInventoryObject(info.inventory, defID))
				return false; // invalid

			// Fall through to the quantity-based handler as the rest is the same
		}

		// Object-based and quantity-based items
		case OBJECT_BASED:
		case QUANTITY_BASED: {
			// Remove item
			removedItemUUIDs = container.inventoryInteraction.removeMultiple(inventory, defID, count);
			if (removedItemUUIDs == null)
				return false;
			break;
		}

		}

		if (player != null) {
			// Send packet if successful
			if (inventory.getAccessor().hasInventoryObject(info.inventory, defID)) {
				JsonArray arr = new JsonArray();
				arr.add(inventory.getAccessor().findInventoryObject(info.inventory, defID));
				InventoryItemPacket pk = new InventoryItemPacket();
				pk.item = arr;
				player.client.sendPacket(pk);
			} else {
				InventoryItemRemovedPacket pk = new InventoryItemRemovedPacket();
				pk.items = removedItemUUIDs;
				player.client.sendPacket(pk);
			}

			// Save items
			for (String itm : inventory.getAccessor().itemsToSave) {
				inventory.setItem(itm, inventory.getItem(itm));

				if (!info.inventory.equals(itm)) {
					// Sync unsaved inventories
					InventoryItemPacket pk = new InventoryItemPacket();
					pk.item = inventory.getItem(itm);
					player.client.sendPacket(pk);
				}
			}
			inventory.getAccessor().completedSave();
		}

		// Return success
		return true;
	}

	/**
	 * Removes items by object
	 * 
	 * @param object Inventory item object
	 * @return True if successful, false otherwise
	 */
	public boolean remove(JsonObject object) {
		// Find definition
		ItemInfo info = definitions.get(Integer.toString(object.get("defId").getAsInt()));
		if (info == null)
			return false;

		// Find inventory
		if (!inventoryTypeMap.containsKey(info.inventory))
			return false;
		InventoryDefinitionContainer container = inventoryTypeMap.get(info.inventory);

		String[] removedItemUUIDs = null;

		// Find type handler
		switch (container.inventoryType) {

		// Single-item
		case SINGLE_ITEM: {
			// Check if the item is present
			if (!inventory.getAccessor().hasInventoryObject(info.inventory, object.get("defId").getAsInt()))
				return false; // invalid

			String removedItemUUID = container.inventoryInteraction.removeOne(inventory, object);

			// Remove item directly
			if (removedItemUUID == null)
				return false;

			removedItemUUIDs = new String[] { removedItemUUID };
			break;
		}

		// Object-based item
		case OBJECT_BASED: {
			// Remove item directly

			String removedItemUUID = container.inventoryInteraction.removeOne(inventory, object);

			// Remove item directly
			if (removedItemUUID == null)
				return false;

			removedItemUUIDs = new String[] { removedItemUUID };

			break;
		}

		// Quantity-based item
		case QUANTITY_BASED: {
			// Check quantity field
			if (object.has("components") && object.get("components").getAsJsonObject().has("Quantity")) {
				int q = object.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().get("quantity")
						.getAsInt();

				// Remove items
				if (q >= 0) {
					if (!inventory.getAccessor().hasInventoryObject(info.inventory, object.get("defId").getAsInt())) {
						// Invalid
						return false;
					} else {
						// Remove existing
						removedItemUUIDs = container.inventoryInteraction.removeMultiple(inventory,
								object.get("defId").getAsInt(), q);
						if (removedItemUUIDs == null) {
							return false;
						}
					}
				}
			} else {
				// Remove item directly
				String removedItemUUID = container.inventoryInteraction.removeOne(inventory, object);

				// Remove item directly
				if (removedItemUUID == null)
					return false;

				removedItemUUIDs = new String[] { removedItemUUID };
				break;
			}
			break;
		}

		}

		if (player != null) {
			String objId = object.get("id").getAsString();
			if (!inventory.getAccessor().hasInventoryObject(info.inventory, objId)) {
				// Send packet if successful
				InventoryItemRemovedPacket pk = new InventoryItemRemovedPacket();
				pk.items = removedItemUUIDs;
				player.client.sendPacket(pk);
			} else {
				JsonArray arr = new JsonArray();
				arr.add(inventory.getAccessor().findInventoryObject(info.inventory, objId));

				// Send packet if successful
				InventoryItemPacket pk = new InventoryItemPacket();
				pk.item = arr;
				player.client.sendPacket(pk);

			}

			// Save items
			for (String itm : inventory.getAccessor().itemsToSave) {
				inventory.setItem(itm, inventory.getItem(itm));

				// Sync unsaved inventories
				InventoryItemPacket pkt = new InventoryItemPacket();
				pkt.item = inventory.getItem(itm);
				player.client.sendPacket(pkt);
			}
			inventory.getAccessor().completedSave();
		}

		// Return success
		return true;
	}

	/**
	 * Retrieves the quantity of the given item currently in the player's inventory
	 * 
	 * @param defID Item defID to retrieve the count of
	 * @return Item count
	 */
	public int getCountOfItem(int defID) {
		// Find definition
		ItemInfo info = definitions.get(Integer.toString(defID));
		if (info == null)
			return 0;

		// Check if the inventory is present
		if (!inventory.containsItem(info.inventory))
			return 0;

		// Find inventory
		if (!inventoryTypeMap.containsKey(info.inventory))
			return 0;
		InventoryDefinitionContainer container = inventoryTypeMap.get(info.inventory);

		// Find type handler
		switch (container.inventoryType) {

		// Single-item
		case SINGLE_ITEM: {
			// Check if the item is present
			if (inventory.getAccessor().hasInventoryObject(info.inventory, defID))
				return 1;
			else
				return 0;
		}

		// Object-based
		case OBJECT_BASED: {
			int count = 0;

			// Find all items of this type
			JsonArray items = inventory.getItem(info.inventory).getAsJsonArray();
			for (JsonElement ele : items) {
				JsonObject itm = ele.getAsJsonObject();
				if (itm.get("defId").getAsInt() == defID) {
					count++;
				}
			}

			return count;
		}

		// Quantity-based
		case QUANTITY_BASED: {
			// Find item
			if (!inventory.getAccessor().hasInventoryObject(info.inventory, defID))
				return 0; // Not present
			JsonObject obj = inventory.getAccessor().findInventoryObject(info.inventory, defID);
			if (!obj.has("components") || !obj.get("components").getAsJsonObject().has("Quantity"))
				return 0; // Invalid

			// Return the quanity directly
			return obj.get("components").getAsJsonObject().get("Quantity").getAsJsonObject().get("quantity").getAsInt();
		}

		}

		// Fallback
		return 0;

	}
}
