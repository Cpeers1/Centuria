package org.asf.centuria.packets.xt.gameserver.shop;

import java.io.IOException;
import java.util.ArrayList;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class ShopItemUnlockPacket implements IXtPacket<ShopItemUnlockPacket> {

	public boolean success = false;

	public String shopId;
	public ArrayList<String> items = new ArrayList<String>();

	@Override
	public ShopItemUnlockPacket instantiate() {
		return new ShopItemUnlockPacket();
	}

	@Override
	public String id() {
		return "$ui";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(DATA_PREFIX); // Data prefix
		writer.writeInt(success ? 1 : 0);
		if (success) {
			writer.writeString(shopId);
			writer.writeInt(items.size());
			items.forEach(t -> writer.writeString(t));
		}
		writer.writeString(DATA_SUFFIX); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return false;
	}

}
