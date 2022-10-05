package org.asf.centuria.packets.xt.gameserver.minigame;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class MinigamePrizePacket implements IXtPacket<MinigamePrizePacket> {

	private static final String PACKET_ID = "mpz";

	public String itemDefId;
	public int itemCount;
	public boolean given;
	public int prizeIndex1;
	public int prizeIndex2;

	@Override
	public MinigamePrizePacket instantiate() {
		return new MinigamePrizePacket();
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
		writer.writeString(itemDefId);
		writer.writeInt(itemCount);
		writer.writeInt(given ? 1 : 0);
		writer.writeInt(prizeIndex1);
		writer.writeInt(prizeIndex2);
		writer.writeString(DATA_SUFFIX); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return true;
	}

}
