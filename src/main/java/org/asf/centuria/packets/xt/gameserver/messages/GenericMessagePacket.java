package org.asf.centuria.packets.xt.gameserver.messages;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class GenericMessagePacket implements IXtPacket<GenericMessagePacket> {

	public String message;
	public String playerID;

	@Override
	public String id() {
		return "dgm";
	}

	@Override
	public GenericMessagePacket instantiate() {
		return new GenericMessagePacket();
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		playerID = reader.read();
		message = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeString("-1");
		writer.writeString(playerID);
		writer.writeString(message);
		writer.writeString("");
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Find player
		GameServer srv = (GameServer) client.getServer();
		Player source = (Player) client.container;
		Player target = srv.getPlayer(playerID);
		if (target != null) {
			// Send
			playerID = source.account.getAccountID();
			target.client.sendPacket(this);
		}
		return true;
	}

}
