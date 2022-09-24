package org.asf.centuria.packets.xt.gameserver.item;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class GiftRedeemPacket implements IXtPacket<GiftRedeemPacket> {

	private static final String PACKET_ID = "gr";
	private String payload;

	@Override
	public GiftRedeemPacket instantiate() {
		return new GiftRedeemPacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		payload = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(DATA_PREFIX);
		writer.writeString(payload);
		writer.writeString(DATA_SUFFIX);
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Just send it back we wont give the items AFTER the popup, we give em before
		client.sendPacket(this);
		return true;
	}

}
