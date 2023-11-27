package org.asf.centuria.tools.legacyclienttools.packets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.tools.legacyclienttools.translation.InventoryTranslators;

public class ProxiedInventoryListPacket implements IXtPacket<ProxiedInventoryListPacket> {

	private String item;

	@Override
	public ProxiedInventoryListPacket instantiate() {
		return new ProxiedInventoryListPacket();
	}

	@Override
	public String id() {
		return "il";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		reader.read();
		// Parse IL
		ByteArrayInputStream ip = new ByteArrayInputStream(Base64.getDecoder().decode(reader.read()));
		GZIPInputStream gz = new GZIPInputStream(ip);
		item = new String(gz.readAllBytes(), "UTF-8");
		gz.close();
		ip.close();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		// Write IL
		ByteArrayOutputStream op = new ByteArrayOutputStream();
		GZIPOutputStream gz = new GZIPOutputStream(op);
		gz.write(item.getBytes("UTF-8"));
		gz.close();
		op.close();

		writer.writeInt(DATA_PREFIX); // Data prefix
		writer.writeString(Base64.getEncoder().encodeToString(op.toByteArray()));
		writer.writeString(DATA_SUFFIX); // Empty suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		item = InventoryTranslators.translateILToBeta(item);
		
		// Send forward
		((SmartfoxClient) client.container).sendPacket(this);
		
		return true;
	}

}
