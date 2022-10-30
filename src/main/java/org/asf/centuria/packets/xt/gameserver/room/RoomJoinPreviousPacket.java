package org.asf.centuria.packets.xt.gameserver.room;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class RoomJoinPreviousPacket implements IXtPacket<RoomJoinPreviousPacket> {

	private static final String PACKET_ID = "rjp";

	@Override
	public RoomJoinPreviousPacket instantiate() {
		return new RoomJoinPreviousPacket();
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
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Teleport back
		Player plr = (Player) client.container;
		plr.teleportToPreviousRoom();
		return true;
	}

}
