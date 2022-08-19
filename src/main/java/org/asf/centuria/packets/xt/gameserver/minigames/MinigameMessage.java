package org.asf.centuria.packets.xt.gameserver.minigames;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.players.Player;
import org.asf.centuria.minigames.TwiggleBuilders;

public class MinigameMessage implements IXtPacket<MinigameMessage> {

	private static final String PACKET_ID = "mm";

    public String command;
    public String data;

	@Override
	public MinigameMessage instantiate() {
		return new MinigameMessage();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
        command = reader.read();
		data = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {

		Player plr = (Player) client.container;

		// Log
		if (System.getProperty("debugMode") != null) {
			System.out.println(
					"[MINIGAME] [MESSAGE] Client to server (command: " + command + ")");
		}

		switch (plr.levelID) {
			case 4111: {
				TwiggleBuilders.HandleMessage(plr, command, data);
				break;
			}
		}

		return true;
	}

}
