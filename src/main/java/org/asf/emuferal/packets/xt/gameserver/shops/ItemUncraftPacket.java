package org.asf.emuferal.packets.xt.gameserver.shops;

import java.io.IOException;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;
import org.asf.emuferal.shops.ShopManager;
import org.asf.emuferal.shops.info.UncraftingInfo;

import com.google.gson.JsonObject;

public class ItemUncraftPacket implements IXtPacket<ItemUncraftPacket> {

	public boolean success = true;
	public String itemId;
	public int count;

	@Override
	public ItemUncraftPacket instantiate() {
		return new ItemUncraftPacket();
	}

	@Override
	public String id() {
		return "iu";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		itemId = reader.read();
		count = reader.readInt();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		writer.writeBoolean(success);
		writer.writeString(itemId);

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Uncraft item
		success = true;
		Player plr = (Player) client.container;
		PlayerInventory inv = plr.account.getPlayerInventory();

		// Find item
		String invId = inv.getAccessor().getInventoryIDOfItem(itemId);
		if (invId != null && inv.getAccessor().hasInventoryObject(invId, itemId)) {
			JsonObject obj = inv.getAccessor().findInventoryObject(invId, itemId);
			String defId = obj.get("defId").getAsString();

			// Retrieve uncraft results
			UncraftingInfo info = ShopManager.getUncraft(defId);
			if (info == null) {
				// Return failure
				success = false;
				client.sendPacket(this);
				return true;
			}

			// Uncraft
			if (count == 1 && !inv.getItemAccessor(plr).remove(obj))
				success = false;
			else if (count != 1 && !inv.getItemAccessor(plr).remove(Integer.parseInt(defId), count))
				success = false;
			if (!success) {
				// Return failure
				client.sendPacket(this);
				return true;
			}

			// Add items
			info.result.forEach((item, count) -> {
				inv.getItemAccessor(plr).add(Integer.parseInt(item), count * this.count);
			});
		} else {
			// Return failure
			success = false;
		}

		client.sendPacket(this);
		return true;
	}

}
