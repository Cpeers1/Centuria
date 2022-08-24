package org.asf.centuria.packets.xt.gameserver.inventory;

import java.io.IOException;

import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class InventoryItemRemovedPacket implements IXtPacket<InventoryItemRemovedPacket> {

	private static final String PACKET_ID = "ilr";

	public String[] items;

	@Override
	public InventoryItemRemovedPacket instantiate() {
		return new InventoryItemRemovedPacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) {
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		// Log
		if (Centuria.debugMode) {
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
