package org.asf.emuferal.packets.xt.gameserver.world;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class JoinRoom implements IXtPacket<JoinRoom> {

	public int roomID = 0;
	public int roomType = 0; // guessing
	public String roomIdentifier = "0";
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
		roomID = reader.readInt();
		roomType = reader.readInt();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		writer.writeBoolean(true); // Success
		writer.writeInt(roomID); // Room ID
		writer.writeInt(roomType); // Room type
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
		join.roomType = roomType;
		join.roomID = roomID;

		// Sync
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				plr.destroyAt(player);
			}
		}

		// Assign room
		plr.roomReady = false;
		plr.pendingRoomID = roomID;
		plr.pendingRoom = "room_" + roomID;
		plr.roomType = roomType;
		join.roomIdentifier = "room_" + roomID;

		// Send response
		client.sendPacket(join);

		return true;
	}

}
