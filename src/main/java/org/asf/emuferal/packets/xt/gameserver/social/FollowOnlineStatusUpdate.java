package org.asf.emuferal.packets.xt.gameserver.social;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class FollowOnlineStatusUpdate implements IXtPacket<FollowOnlineStatusUpdate> {

	private String message;

	@Override
	public FollowOnlineStatusUpdate instantiate() {
		return new FollowOnlineStatusUpdate();
	}

	@Override
	public String id() {
		return "rfosu";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		message = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		// TODO: verify this
		writer.writeInt(-1); // Data prefix

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Interaction start

	
		//log interaction details
		if (System.getProperty("debugMode") != null) {
			System.out.println("[SOCIAL] [FollowOnlineStatusUpdate]  Client to server (" + message + ")");
		}
		
		// TODO
		return true;
	}

}
