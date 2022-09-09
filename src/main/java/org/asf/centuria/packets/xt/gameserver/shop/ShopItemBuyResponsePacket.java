package org.asf.centuria.packets.xt.gameserver.shop;

import java.io.IOException;
import java.util.ArrayList;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.shops.BoughtItemInfo;
import org.asf.centuria.enums.shops.ItemBuyStatus;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class ShopItemBuyResponsePacket implements IXtPacket<ShopItemBuyResponsePacket> {

	private static final String PACKET_ID = "$b";

	public ItemBuyStatus status;
	public ArrayList<BoughtItemInfo> items = new ArrayList<BoughtItemInfo>();
	public ArrayList<BoughtItemInfo> eurekaItems = new ArrayList<BoughtItemInfo>();

	@Override
	public ShopItemBuyResponsePacket instantiate() {
		return new ShopItemBuyResponsePacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(DATA_PREFIX); // Data prefix

		writer.writeInt(status.getStatus());
		writer.writeInt(items.size());
		items.forEach(t -> {
			writer.writeString(t.itemID);
			writer.writeInt(t.count);
		});
		if (!eurekaItems.isEmpty()) {
			writer.writeInt(eurekaItems.size());
			eurekaItems.forEach(t -> {
				writer.writeString(t.itemID);
				writer.writeInt(t.count);
			});
		}

		writer.writeString(DATA_SUFFIX); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return false;
	}

}
