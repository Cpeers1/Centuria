package org.asf.emuferal.packets.xt.gameserver.objects;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
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
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Avatar action
		Player plr = (Player) client.container;
		XtWriter pk = new XtWriter();
		pk.writeString("aa");
		pk.writeInt(-1); // Data prefix
		pk.writeString(plr.account.getAccountID());
		pk.writeLong(System.currentTimeMillis() / 1000);
		pk.writeString(action);
		pk.writeString(""); // Data suffix

		// Broadcast sync
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				player.client.sendPacket(pk.encode());
			}
		}

		// TODO
		return true;
	}

}
