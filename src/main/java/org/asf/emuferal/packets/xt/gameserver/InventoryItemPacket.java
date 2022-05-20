package org.asf.emuferal.packets.xt.gameserver;

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

	public JsonElement item;

	@Override
	public InventoryItemPacket instantiate() {
		return new InventoryItemPacket();
	}

	@Override
	public String id() {
		return "il";
	}

	@Override
	public void parse(XtReader reader) {
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		ByteArrayOutputStream op = new ByteArrayOutputStream();
		GZIPOutputStream gz = new GZIPOutputStream(op);
		gz.write(item.toString().getBytes("UTF-8"));
		gz.close();
		op.close();

		writer.writeInt(-1); // Data prefix
		writer.writeString(Base64.getEncoder().encodeToString(op.toByteArray()));
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return false;
	}

}
