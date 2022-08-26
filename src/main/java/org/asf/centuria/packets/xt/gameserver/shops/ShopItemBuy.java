package org.asf.centuria.packets.xt.gameserver.shops;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import org.asf.centuria.accounts.highlevel.ItemAccessor;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.entities.shops.BoughtItemInfo;
import org.asf.centuria.enums.shops.ItemBuyStatus;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.centuria.shops.ShopManager;
import org.asf.centuria.shops.info.ShopItem;
import org.asf.centuria.util.WeightedSelectorUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ShopItemBuy implements IXtPacket<ShopItemBuy> {

	private static final String PACKET_ID = "$b";
	
	private String item;
	private String shopType;
	private int count;

	@Override
	public ShopItemBuy instantiate() {
		return new ShopItemBuy();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		item = reader.read();
		shopType = reader.read();
		count = reader.readInt();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Buy item
		Player plr = (Player) client.container;
		ItemAccessor acc = plr.account.getPlayerInventory().getItemAccessor(plr);

		// Find item
		ShopItem itm = ShopManager.getShopItemInfo(plr.account, shopType, item, count);
		if (itm == null) {
			// Item unavailable
			ItemBuyResponse pkt = new ItemBuyResponse();
			pkt.status = ItemBuyStatus.UNAVAILABLE;
			client.sendPacket(pkt);
			return true;
		}

		// Check if the player can afford the item
		for (String itemId : itm.cost.keySet()) {
			int cost = itm.cost.get(itemId) * count;
			if (acc.getCountOfItem(Integer.parseInt(itemId)) < cost) {
				// Item unaffordable
				ItemBuyResponse pkt = new ItemBuyResponse();
				pkt.status = ItemBuyStatus.UNAFFORDABLE;
				client.sendPacket(pkt);
				return true;
			}
		}

		// Check level lock
		if (itm.requiredLevel != -1 && (!plr.account.getLevel().isLevelAvailable()
				|| plr.account.getLevel().getLevel() < itm.requiredLevel)) {
			ItemBuyResponse pkt = new ItemBuyResponse();
			pkt.status = ItemBuyStatus.LEVEL_LOCKED;
			client.sendPacket(pkt);
			return true;
		}

		// Buy item
		ItemBuyResponse res = new ItemBuyResponse();
		res.status = ItemBuyStatus.SUCCESS;
		for (String itemId : itm.cost.keySet()) {
			int cost = itm.cost.get(itemId) * count;

			// Remove items
			acc.remove(Integer.parseInt(itemId), cost);
		}
		itm.items.forEach((id, amount) -> {
			amount *= count;

			// Add item to inventory
			String[] ids;
			if (!id.equals("30197"))
				ids = acc.add(Integer.parseInt(id), amount);
			else {
				// Look slots

				// Add the slot
				plr.account.getPlayerInventory().getAvatarAccessor().addExtraLookSlot();

				// Create fake update
				String slotID = UUID.randomUUID().toString();
				JsonArray arr = new JsonArray();
				JsonObject components = new JsonObject();

				// Add timestamp
				JsonObject ts = new JsonObject();
				ts.addProperty("ts", System.currentTimeMillis());
				components.add("Timestamp", ts);

				// Build object
				JsonObject obj = new JsonObject();
				obj.addProperty("defId", 30197);
				obj.add("components", components);
				obj.addProperty("id", slotID);
				obj.addProperty("type", 315);
				arr.add(obj);

				// Send slot update
				InventoryItemPacket pk = new InventoryItemPacket();
				pk.item = arr;
				client.sendPacket(pk);

				// Send look update
				pk = new InventoryItemPacket();
				pk.item = plr.account.getPlayerInventory().getItem("avatars");
				client.sendPacket(pk);

				// Save ID so the astrale shop wont freeze
				ids = new String[] { slotID };

				// Save items
				for (String itmTS : plr.account.getPlayerInventory().getAccessor().getItemsToSave())
					plr.account.getPlayerInventory().setItem(itmTS, plr.account.getPlayerInventory().getItem(itmTS));
				plr.account.getPlayerInventory().getAccessor().completedSave();
			}
			for (String itemId : ids) {
				BoughtItemInfo i = new BoughtItemInfo();
				i.count = amount;
				i.itemID = itemId;
				res.items.add(i);
			}
		});

		// Eureka items
		if (!itm.eurekaItems.isEmpty()) {
			// Build a weight map
			int currentWeight = 0;
			HashMap<String, Integer> weights = new HashMap<String, Integer>();
			for (String item : itm.eurekaItems.keySet()) {
				int chance = itm.eurekaItems.get(item);
				weights.put(item, chance);
				currentWeight += chance;
			}
			if (currentWeight < 100) {
				weights.put(null, 100 - currentWeight);
			}

			// Retrieve entry
			String item = WeightedSelectorUtil.select(weights);
			if (item != null) {
				// Give eureka item
				BoughtItemInfo e = new BoughtItemInfo();
				e.itemID = acc.add(Integer.parseInt(item));
				e.count = 1;
				res.eurekaItems.add(e);
			}
		}

		// Send packets
		client.sendPacket(res);

		// Update shop log
		ShopManager.purchaseCompleted(plr.account, shopType, item, count);

		// Shop list
		ShopList resp = new ShopList();
		resp.shopType = shopType;
		resp.items = ShopManager.getShopContents(((Player) client.container).account, shopType);
		client.sendPacket(resp);

		return true;
	}

}
