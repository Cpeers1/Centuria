package org.asf.emuferal.packets.xt.gameserver.inventory;

import java.io.IOException;
import java.util.HashMap;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

import com.google.gson.JsonObject;

public class InventoryItemUseDye implements IXtPacket<InventoryItemUseDye> {

	public String itemID;
	public HashMap<String, Integer> dyes = new HashMap<String, Integer>();

	@Override
	public InventoryItemUseDye instantiate() {
		return new InventoryItemUseDye();
	}

	@Override
	public String id() {
		return "iud";
	}

	@Override
	public void parse(XtReader reader) {
		itemID = reader.read();

		int l = reader.readInt();
		for (int i = 0; i < l; i++) {
			dyes.put(reader.read(), -1);
		}

		for (String dye : dyes.keySet()) {
			dyes.put(dye, reader.readInt() + 1);
		}
	}

	@Override
	public void build(XtWriter writer) {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		Player plr = (Player) client.container;
		PlayerInventory inv = plr.account.getPlayerInventory();

		// Find clothing item
		if (inv.getAccessor().getClothingData(itemID) != null) {
			// Apply dyes
			dyeObject(inv.getAccessor().getClothingData(itemID), inv);

			// Save clothes
			inv.setItem("100", inv.getItem("100"));

			// Update object in client inventory
			InventoryItemPacket pkt = new InventoryItemPacket();
			pkt.item = inv.getItem("100");
			client.sendPacket(pkt);
		}

		// TODO: sanctuary items

		// Update dyes object in client inventory
		InventoryItemPacket pkt = new InventoryItemPacket();
		pkt.item = inv.getItem("111");
		client.sendPacket(pkt);

		return true;
	}

	private void dyeObject(JsonObject item, PlayerInventory inv) {
		// Find color object
		if (item.has("components") && item.get("components").getAsJsonObject().has("Colorable")) {
			// Apply dyes
			JsonObject target = item.get("components").getAsJsonObject().get("Colorable").getAsJsonObject();
			for (String dye : dyes.keySet()) {
				// Apply dye color
				String hsv = inv.getAccessor().getDyeHSV(inv.getAccessor().getDyeData(dye).get("defId").getAsInt());
				String obj = "color" + dyes.get(dye) + "HSV";
				if (target.has(obj) && hsv != null) {
					target.get(obj).getAsJsonObject().remove("_hsv");
					target.get(obj).getAsJsonObject().addProperty("_hsv", hsv);
				}

				// Remove dye from inventory
				inv.getAccessor().removeDye(dye);
			}
		}

		// Save changes
		for (String change : inv.getAccessor().getItemsToSave())
			inv.setItem(change, inv.getItem(change));
		inv.getAccessor().completedSave();
	}

}
