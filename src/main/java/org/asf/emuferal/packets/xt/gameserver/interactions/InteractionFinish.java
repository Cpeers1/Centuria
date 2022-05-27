package org.asf.emuferal.packets.xt.gameserver.interactions;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class InteractionFinish implements IXtPacket<InteractionFinish> {

	private String target;

	@Override
	public InteractionFinish instantiate() {
		return new InteractionFinish();
	}

	@Override
	public String id() {
		return "oaf";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		target = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Interaction finish
		Player plr = (Player) client.container;

		if (System.getProperty("debugMode") != null) {
			System.out.println("[INTERACTION] [FINISH] Client to server (target: " + target + ")");
		}
		
		// TODO
		return true;
	}

}
