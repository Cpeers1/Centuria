package org.asf.centuria.packets.xt.gameserver;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class PrefixedPacket implements IXtPacket<PrefixedPacket> {

	private String packetID;
	private String subPacket;

	@Override
	public PrefixedPacket instantiate() {
		return new PrefixedPacket();
	}

	@Override
	public String id() {
		return "o";
	}

	@Override
	public void parse(XtReader reader) {
		packetID = reader.read();
		reader.read(); // data prefix (always -1)

		// For followup content
		subPacket = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) {
		writer.writeString(packetID);
		writer.writeInt(-1); // data prefix (always -1)
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		String data = "%xt%" + packetID + "%" + subPacket + "%";
		return client.getServer().handlePacket(data, client);
	}

}
