package org.asf.emuferal.packets.xt.gameserver.shops;

import java.io.IOException;
import java.util.ArrayList;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.entities.shops.BoughtItemInfo;
import org.asf.emuferal.enums.shops.ItemBuyStatus;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;

public class ItemBuyResponse implements IXtPacket<ItemBuyResponse> {

	private static final String PACKET_ID = "$b";

	public ItemBuyStatus status;
	public ArrayList<BoughtItemInfo> items = new ArrayList<BoughtItemInfo>();
	public ArrayList<BoughtItemInfo> eurekaItems = new ArrayList<BoughtItemInfo>();

	@Override
	public ItemBuyResponse instantiate() {
		return new ItemBuyResponse();
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
		writer.writeInt(-1); // Data prefix

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

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return false;
	}

}
