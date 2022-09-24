package org.asf.centuria.packets.xt.gameserver.inventory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

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
		Centuria.logger.debug(MarkerManager.getMarker("ITEMUPDATE"), "Server to client: " + item);

		ByteArrayOutputStream op = new ByteArrayOutputStream();
		GZIPOutputStream gz = new GZIPOutputStream(op);
		gz.write(item.toString().getBytes("UTF-8"));
		gz.close();
		op.close();

		writer.writeInt(DATA_PREFIX); // Data prefix
		writer.writeString(Base64.getEncoder().encodeToString(op.toByteArray()));
		writer.writeString(DATA_SUFFIX); // Empty suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return false;
	}

}
