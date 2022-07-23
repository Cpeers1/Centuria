package org.asf.emuferal.packets.xt.gameserver.sanctuaries;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.packets.xt.gameserver.world.JoinRoom;
import org.asf.emuferal.players.Player;

public class SanctuaryUpgradeStart implements IXtPacket<SanctuaryUpgradeStart> {

	private static final String PACKET_ID = "sus";

	public String lookId = null;

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public SanctuaryUpgradeStart instantiate() {
		return new SanctuaryUpgradeStart();
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		lookId = reader.read();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix


		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Switch sanctuary look


		return true;
	}

}
