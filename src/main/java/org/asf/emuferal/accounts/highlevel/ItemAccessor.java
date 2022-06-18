package org.asf.emuferal.accounts.highlevel;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.itemdata.InventoryType;
import org.asf.emuferal.accounts.highlevel.itemdata.ItemInfo;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.emuferal.players.Player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ItemAccessor {

	private Player player;
	private PlayerInventory inventory;
	private static HashMap<String, ItemInfo> definitions = new HashMap<String, ItemInfo>();

	@SuppressWarnings("serial")
	private static final Map<String, InventoryType> inventoryTypeMap = new HashMap<String, InventoryType>() {
		{
			// Avatar species
			put("1", InventoryType.SINGLE_ITEM);

			// Body mods
			put("2", InventoryType.SINGLE_ITEM);

			// Clothing
			put("100", InventoryType.OBJECT_BASED);

			// Sanctuary classes
			put("10", InventoryType.SINGLE_ITEM);

			// Sanctuary houses
			put("5", InventoryType.OBJECT_BASED);

			// Sanctuary islands
			put("6", InventoryType.OBJECT_BASED);

			// Furniture
			put("102", InventoryType.OBJECT_BASED);

			// Currency
			put("104", InventoryType.QUANTITY_BASED);

			// Resources
			put("103", InventoryType.QUANTITY_BASED);
			// Dyes
			put("111", InventoryType.QUANTITY_BASED);

			// Enigmas (ty to Mewt/Uzukara for pointing this out)
			put("7", InventoryType.OBJECT_BASED);

			// Inspirations
			put("8", InventoryType.SINGLE_ITEM);
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
	 * Adds items by DefID
	 * 
	 * @param defID Item defID
	 * @return Item ID string or null if invalid
	 */
	public String add(int defID) {
		// Find definition
		ItemInfo info = definitions.get(Integer.toString(defID));
		if (info == null)
			return null;

		// Find inventory
		if (!inventoryTypeMap.containsKey(info.inventory))
			return null;
		InventoryType invType = inventoryTypeMap.get(info.inventory);

		// Find type handler
		switch (invType) {

		// Single-item
		case SINGLE_ITEM: {
			// Check if the item is present
			if (inventory.getAccessor().hasInventoryObject(info.inventory, defID))
				return null; // invalid

			// Add item
//			inventory.getAccessor().createObjectFromType;
			break;
		}

		}

		return null;
	}

}
