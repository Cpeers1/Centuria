package org.asf.emuferal.accounts.highlevel;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.accounts.highlevel.itemdata.ItemInfo;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ItemAccessor {

	private PlayerInventory inventory;
	private static HashMap<String, ItemInfo> definitions = new HashMap<String, ItemInfo>();

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
				definitions.put(itemID, info);
			});
		} catch (IOException e) {
		}
	}

	public ItemAccessor(PlayerInventory inventory) {
		this.inventory = inventory;
	}

}
