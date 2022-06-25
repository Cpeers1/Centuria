package org.asf.emuferal.packets.xt.gameserver.inventory;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;

public class InventoryItemRemovedPacket implements IXtPacket<InventoryItemRemovedPacket> {

	public String[] items;

	@Override
	public InventoryItemRemovedPacket instantiate() {
		return new InventoryItemRemovedPacket();
	}

	@Override
	public String id() {
		return "ilr";
	}

	@Override
	public void parse(XtReader reader) {
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		// Log
		if (System.getProperty("debugMode") != null) {
			String itms = "";
			for (String itm : items)
				if (itms.isEmpty())
					itms = itm;
				else
					itms += ", " + itm;
			System.out.println("[INVENTORY] [REMOVE]  Server to client: " + itms);
		}

		writer.writeInt(-1); // Data prefix
		writer.writeInt(items.length);
		for (String itm : items)
			writer.writeString(itm);
		writer.writeString(""); // Empty suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return false;
	}

}
