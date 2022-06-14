package org.asf.emuferal.packets.xt.gameserver.world;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class JoinRoom implements IXtPacket<JoinRoom> {

	public int levelID = 0;
	public int levelType = 0;
	public String roomIdentifier = "room_0";
	public String teleport = "";

	@Override
	public JoinRoom instantiate() {
		return new JoinRoom();
	}

	@Override
	public String id() {
		return "rj";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		levelID = reader.readInt();
		levelType = reader.readInt();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		writer.writeBoolean(true); // Success
		writer.writeInt(levelID); // Room ID
		writer.writeInt(levelType); // Room type
		writer.writeInt(-1); // Iss Room ID (unused as we dont support it)
		writer.writeString(teleport); // Specific teleport
		writer.writeString(roomIdentifier); // Chat room ID

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Load the requested room
		Player plr = (Player) client.container;

		JoinRoom join = new JoinRoom();
		join.levelType = levelType;
		join.levelID = levelID;

		// Sync
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				plr.destroyAt(player);
			}
		}

		// Assign room
		plr.roomReady = false;
		plr.pendingLevelID = levelID;
		plr.pendingRoom = "room_" + levelID;
		plr.levelType = levelType;
		join.roomIdentifier = plr.pendingRoom;

		// Log
		if (System.getProperty("debugMode") != null) {
			System.out.println("[JOINROOM]  Client to server (room: " + plr.pendingRoom + ", level: " + plr.pendingLevelID + ")");
		}

		// Send response
		client.sendPacket(join);

		return true;
	}

}
