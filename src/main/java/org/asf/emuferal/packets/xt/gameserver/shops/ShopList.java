package org.asf.emuferal.packets.xt.gameserver.shops;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;
import org.asf.emuferal.shops.ShopManager;

public class ShopList implements IXtPacket<ShopList> {

	public String shopType;
	public String[] items;

	@Override
	public ShopList instantiate() {
		return new ShopList();
	}

	@Override
	public String id() {
		return "$l";
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
