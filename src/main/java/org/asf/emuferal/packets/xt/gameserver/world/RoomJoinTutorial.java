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
		plr.pendingLevelID = 25280;
		plr.pendingRoom = "room_25280";
		plr.levelType = 4;

		// Send response
		JoinRoom join = new JoinRoom();
		join.levelType = plr.levelType;
		join.levelID = plr.pendingLevelID;
		client.sendPacket(join);

		// Log
		if (System.getProperty("debugMode") != null) {
			System.out.println("[JOINROOM]  Client to server (room: " + plr.pendingRoom + ", level: " + plr.pendingLevelID + ")");
		}

		return true;
	}

}
