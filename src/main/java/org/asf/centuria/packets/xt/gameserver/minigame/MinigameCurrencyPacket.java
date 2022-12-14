package org.asf.centuria.packets.xt.gameserver.minigame;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class MinigameCurrencyPacket implements IXtPacket<MinigameCurrencyPacket> {

	private static final String PACKET_ID = "mg";

	public int Currency;
	public int UNK1 = 1;
	public String UNK2 = "null";

	@Override
	public MinigameCurrencyPacket instantiate() {
		return new MinigameCurrencyPacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(DATA_PREFIX); // padding

		writer.writeInt(Currency);
		writer.writeInt(UNK1);
		writer.writeString(UNK2);

		writer.writeString(DATA_SUFFIX); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return true;
	}

}
