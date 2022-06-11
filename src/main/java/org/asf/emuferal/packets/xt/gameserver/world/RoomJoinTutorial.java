package org.asf.emuferal.packets.xt.gameserver.world;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class RoomJoinTutorial implements IXtPacket<RoomJoinTutorial> {

	@Override
	public RoomJoinTutorial instantiate() {
		return new RoomJoinTutorial();
	}

	@Override
	public String id() {
		return "rjt";
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

		// Assign room
		plr.roomReady = false;
		plr.pendingRoomID = 25280;
		plr.pendingRoom = "room_25280";
		plr.roomType = 4;

		// Send response
		JoinRoom join = new JoinRoom();
		join.roomType = plr.roomType;
		join.roomID = plr.pendingRoomID;
		client.sendPacket(join);

		return true;
	}

}
