package org.asf.centuria.packets.xt.gameserver.inventory;

import java.io.IOException;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.players.Player;

import com.google.gson.JsonObject;

public class InventoryItemTradeListUpdate implements IXtPacket<InventoryItemTradeListUpdate> {

	private static final String PACKET_ID = "tlu";

	public String itemID;
	public boolean add = true;

	@Override
	public InventoryItemTradeListUpdate instantiate() {
		return new InventoryItemTradeListUpdate();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) {
		itemID = reader.read();
		add = reader.readInt() == 1;
	}

	@Override
	public void build(XtWriter writer) {
		writer.writeInt(-1); // Data prefix

		writer.writeString(itemID);
		writer.writeInt(add ? 1 : 0);

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		Player plr = (Player) client.container;
		PlayerInventory inv = plr.account.getPlayerInventory();

		// Find inventory ID
		String invType = inv.getAccessor().getInventoryIDOfItem(itemID);

		// Handle packet
		if (invType != null) {
			// Find object
			JsonObject item = inv.getAccessor().findInventoryObject(invType, itemID);
			if (item != null) {
				JsonObject components = item.get("components").getAsJsonObject();
				if (!components.has("Tradable"))
					components.add("Tradable", new JsonObject());
				JsonObject t = components.get("Tradable").getAsJsonObject();
				if (t.has("isInTradeList"))
					t.remove("isInTradeList");
				t.addProperty("isInTradeList", add);
				inv.setItem(invType, inv.getItem(invType));
			}
		}

		// Send response
		client.sendPacket(this);

		return true;
	}

}
