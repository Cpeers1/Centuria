package org.asf.emuferal.packets.xt.gameserver.objects;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class WorldObjectUpdate implements IXtPacket<WorldObjectUpdate> {

	private String data;

	@Override
	public WorldObjectUpdate instantiate() {
		return new WorldObjectUpdate();
	}

	@Override
	public String id() {
		return "ou";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		data = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data suffix

		writer.writeString(data);
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Object update
		Player plr = (Player) client.container;

		// TODO
		return true;
	}

}
