package org.asf.centuria.packets.xt.gameserver.shops;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.players.Player;
import org.asf.centuria.shops.ShopManager;

public class ShopList implements IXtPacket<ShopList> {

	private static final String PACKET_ID = "$l";

	public String shopType;
	public String[] items;

	@Override
	public ShopList instantiate() {
		return new ShopList();
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
		writer.writeInt(-1); // Data prefix

		writer.writeString(shopType);
		writer.writeInt(items.length);
		for (String itm : items)
			writer.writeString(itm);

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Shop list

		ShopList resp = new ShopList();
		resp.shopType = shopType;
		resp.items = ShopManager.getShopContents(((Player) client.container).account, shopType);
		client.sendPacket(resp);

		return true;
	}

}
