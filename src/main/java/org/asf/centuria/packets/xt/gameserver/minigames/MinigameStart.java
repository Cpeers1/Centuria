package org.asf.centuria.packets.xt.gameserver.minigames;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class MinigameStart implements IXtPacket<MinigameStart> {

	private static final String PACKET_ID = "ms";

	public int UNK1 = 1;

	@Override
	public MinigameStart instantiate() {
		return new MinigameStart();
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
		writer.writeInt(UNK1);
		writer.writeString(DATA_SUFFIX); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return true;
	}

}
