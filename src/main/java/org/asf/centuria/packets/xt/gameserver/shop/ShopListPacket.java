package org.asf.centuria.packets.xt.gameserver.shop;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.shops.ShopManager;

public class ShopListPacket implements IXtPacket<ShopListPacket> {

	private static final String PACKET_ID = "$l";

	public String shopType;
	public String[] items;

	@Override
	public ShopListPacket instantiate() {
		return new ShopListPacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		shopType = reader.read();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(DATA_PREFIX); // Data prefix

		writer.writeString(shopType);
		writer.writeInt(items.length);
		for (String itm : items)
			writer.writeString(itm);

		writer.writeString(DATA_SUFFIX); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Shop list

		ShopListPacket resp = new ShopListPacket();
		resp.shopType = shopType;
		resp.items = ShopManager.getShopContents(((Player) client.container).account, shopType);
		client.sendPacket(resp);

		return true;
	}

}
