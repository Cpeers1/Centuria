package org.asf.centuria.packets.xt.gameserver.inventory;

import java.io.IOException;
import java.util.UUID;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.InventoryManager;
import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class InventoryItemDownloadPacket implements IXtPacket<InventoryItemDownloadPacket> {

	private static final String PACKET_ID = "ilt";

	private String slot = "";

	@Override
	public InventoryItemDownloadPacket instantiate() {
		return new InventoryItemDownloadPacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
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
		PlayerInventory inv = plr.account.getSaveSpecificInventory();

		// Log
		Centuria.logger.debug(MarkerManager.getMarker("ITEMREQUEST"), "Client to server (type: " + slot + ")");

		if (slot.equals("400")) {
			// Override wings lock
			if (!inv.containsItem("400"))
				inv.setItem("400", new JsonArray());
			JsonArray itm = inv.getItem("400").getAsJsonArray();

			// Build entry
			JsonObject obj = new JsonObject();
			obj.addProperty("defId", "22441");
			JsonObject components = new JsonObject();
			JsonObject ts = new JsonObject();
			ts.addProperty("ts", System.currentTimeMillis());
			components.add("Timestamp", ts);
			obj.add("components", components);
			obj.addProperty("id", UUID.randomUUID().toString());
			obj.addProperty("type", 400);
			itm.add(obj);

			// Send the item to the client
			InventoryItemPacket pkt = new InventoryItemPacket();
			pkt.item = itm;
			client.sendPacket(pkt);

			return true;
		}

		// Emotes
		if (slot.equals("9")) {
			JsonArray item = new JsonArray();

			// Add all emotes
			InventoryManager.addEmote(item, "9122");
			InventoryManager.addEmote(item, "9151");
			InventoryManager.addEmote(item, "9108");
			InventoryManager.addEmote(item, "9121");
			InventoryManager.addEmote(item, "9143");
			InventoryManager.addEmote(item, "9190");
			InventoryManager.addEmote(item, "8930");
			InventoryManager.addEmote(item, "9116");

			// Send the item to the client
			InventoryItemPacket pkt = new InventoryItemPacket();
			pkt.item = item;
			client.sendPacket(pkt);

			return true;
		}

		// Load the item
		JsonElement item = inv.getItem(slot.equals("200") ? "avatars" : slot);

		// Send the item to the client
		if (item == null)
			item = new JsonArray();

		// Remove invalid items
		if (item instanceof JsonArray) {
			for (JsonElement itm : ((JsonArray) item).deepCopy()) {
				if (itm.isJsonObject() && itm.getAsJsonObject().has("type")
						&& !itm.getAsJsonObject().get("type").getAsString().equals(slot)) {
					((JsonArray) item).remove(itm);
				}
			}
		}

		InventoryItemPacket pkt = new InventoryItemPacket();
		pkt.item = item;
		client.sendPacket(pkt);

		return true;
	}

}
