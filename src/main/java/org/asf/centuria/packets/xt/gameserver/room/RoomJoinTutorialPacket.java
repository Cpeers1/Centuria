package org.asf.centuria.packets.xt.gameserver.room;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class RoomJoinTutorialPacket implements IXtPacket<RoomJoinTutorialPacket> {

	private static final String PACKET_ID = "rjt";
	
	@Override
	public RoomJoinTutorialPacket instantiate() {
		return new RoomJoinTutorialPacket();
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
		// Make the client load the tutorial
		Player plr = (Player) client.container;
		plr.teleportToRoom(25280, 4, -1, "room_25280", "");
		return true;
	}

}
