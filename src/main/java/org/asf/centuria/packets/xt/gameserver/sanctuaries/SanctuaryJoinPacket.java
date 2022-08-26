package org.asf.centuria.packets.xt.gameserver.sanctuaries;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

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
