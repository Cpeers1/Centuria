package org.asf.emuferal.packets.xt.gameserver.world;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class JoinRoom implements IXtPacket<JoinRoom> {

	public int roomID = 0;
	public int playerID = 0;
	public int mode = 0; // guessing
	public String roomIdentifier = "";

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
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		writer.writeBoolean(true); // Not sure tbh
		writer.writeInt(roomID); // Room ID
		writer.writeInt(mode); // Mode?
		writer.writeInt(playerID); // Player ID
		writer.writeString(""); // Idk but its usually empty
		writer.writeString(roomIdentifier); // Room ID?

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

		// Chat room
		join.roomIdentifier = "room_" + roomID;
		client.sendPacket(join);

		return true;
	}

}
