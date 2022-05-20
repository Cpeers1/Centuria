package org.asf.emuferal.packets.xt.gameserver.objects;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class AvatarAction implements IXtPacket<AvatarAction> {

	private String action;
	private String playerUUID;

	@Override
	public AvatarAction instantiate() {
		return new AvatarAction();
	}

	@Override
	public String id() {
		return "aa";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		action = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		// TODO: verify this
		writer.writeInt(-1); // Data prefix

		writer.writeString(playerUUID);
		writer.writeString(action);

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Avatar action
		Player plr = (Player) client.container;

		// TODO
		return true;
	}

}
