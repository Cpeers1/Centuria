package org.asf.emuferal.packets.xt.gameserver.interactions;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class InteractionStart implements IXtPacket<InteractionStart> {

	private String source;
	private String target;

	@Override
	public InteractionStart instantiate() {
		return new InteractionStart();
	}

	@Override
	public String id() {
		return "oas";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		target = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		// TODO: verify this
		writer.writeInt(-1); // Data prefix

		writer.writeString(target);
		writer.writeString(source);

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Interaction start
		Player plr = (Player) client.container;
		source = plr.account.getAccountID();

		// Broadcast interaction
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (player.room != null && plr.room != null && player.room.equals(plr.room)) {
				player.client.sendPacket(this);
			}
		}
		
		//log interaction details
		
		if (System.getProperty("debugMode") != null) {
			System.out.println("[INTERACTION] [START] Client to server (source " + source + ", target: " + target + ")");
		}

		// TODO
		return true;
	}

}
