package org.asf.emuferal.packets.xt.gameserver.sanctuaries;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class SanctuaryJoinPacket implements IXtPacket<SanctuaryJoinPacket> {

	private static final String PACKET_ID = "sj";

	public String sanctuaryOwner = null;
	public int mode = 0;

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public SanctuaryJoinPacket instantiate() {
		return new SanctuaryJoinPacket();
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		sanctuaryOwner = reader.read();
		mode = reader.readInt();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Sanctuary join
		Player player = (Player) client.container;
		player.teleportToSanctuary(sanctuaryOwner);
		return true;
	}

}
