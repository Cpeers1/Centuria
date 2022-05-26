package org.asf.emuferal.packets.xt.gameserver.players;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class JumpToPlayer implements IXtPacket<JumpToPlayer> {

	private String accountID;

	@Override
	public JumpToPlayer instantiate() {
		return new JumpToPlayer();
	}

	@Override
	public String id() {
		return "rfjtr";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		accountID = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Find player
		for (Player plr : ((GameServer) client.getServer()).getPlayers()) {
			if (plr.account.getAccountID().equals(accountID) && plr.roomReady) { // TODO: privacy settings
				XtWriter writer = new XtWriter();
				writer.writeString("rfjtr");
				writer.writeInt(-1); // data prefix
				writer.writeInt(-1); // success
				writer.writeString(""); // data suffix
				client.sendPacket(writer.encode());
				break;
			}
		}

		XtWriter writer = new XtWriter();
		writer.writeString("rfjtr");
		writer.writeInt(-1); // data prefix
		writer.writeInt(0); // failure
		writer.writeString(""); // data suffix
		client.sendPacket(writer.encode());
		return true; // Account not found or loading
	}

}
