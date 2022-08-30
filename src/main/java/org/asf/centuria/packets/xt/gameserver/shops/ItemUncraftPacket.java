package org.asf.centuria.packets.xt.gameserver.shops;

import java.io.IOException;

import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.shops.ShopManager;
import org.asf.centuria.shops.info.UncraftingInfo;

import com.google.gson.JsonObject;

public class ItemUncraftPacket implements IXtPacket<ItemUncraftPacket> {

	private static final String PACKET_ID = "iu";
	
	public boolean success = true;
	public String itemId;
	public int count;

	@Override
	public ItemUncraftPacket instantiate() {
		return new ItemUncraftPacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
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

			// Check count
			if (inv.getItemAccessor(plr).getCountOfItem(Integer.parseInt(defId)) >= count) {
				client.sendPacket(this);

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
				return true;
			}

			// Return failure
			success = false;
		} else {
			// Return failure
			success = false;
		}

		client.sendPacket(this);
		return true;
	}

}
