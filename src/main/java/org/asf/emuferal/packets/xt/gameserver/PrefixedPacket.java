package org.asf.emuferal.packets.xt.gameserver;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;

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
