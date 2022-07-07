package org.asf.emuferal.packets.xt.gameserver.inventory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;

import com.google.gson.JsonElement;

public class InventoryItemPacket implements IXtPacket<InventoryItemPacket> {

	private static final String PACKET_ID = "il";

	public JsonElement item;

	@Override
	public InventoryItemPacket instantiate() {
		return new InventoryItemPacket();
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
		if (System.getProperty("debugMode") != null) {
			System.out.println("[INVENTORY] [UPDATE]  Server to client: " + item);
		}

		ByteArrayOutputStream op = new ByteArrayOutputStream();
		GZIPOutputStream gz = new GZIPOutputStream(op);
		gz.write(item.toString().getBytes("UTF-8"));
		gz.close();
		op.close();

		writer.writeInt(-1); // Data prefix
		writer.writeString(Base64.getEncoder().encodeToString(op.toByteArray()));
		writer.writeString(""); // Empty suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return false;
	}

}
