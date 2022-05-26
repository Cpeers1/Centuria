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
	public int playerID = 0;
	public int mode = 0; // guessing
	public String roomIdentifier = "0";

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
		mode = reader.readInt();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		writer.writeBoolean(true); // Success
		writer.writeInt(roomID); // Room ID
		writer.writeInt(mode); // Mode?
		writer.writeInt(playerID); // Player ID
		writer.writeString(""); // Idk but its usually empty
		writer.writeString(roomIdentifier); // Chat room ID

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Load the requested room
		Player plr = (Player) client.container;

		JoinRoom join = new JoinRoom();
		join.mode = 0;
		join.roomID = roomID;
		join.playerID = plr.account.getAccountNumericID();

		// Sync
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				plr.destroyAt(player);
			}
		}

		// Assign room
		plr.room = "room_" + roomID;
		plr.roomReady = false;

		// Chat room
		join.roomIdentifier = "room_" + roomID;
		client.sendPacket(join);

		return true;
	}

}
