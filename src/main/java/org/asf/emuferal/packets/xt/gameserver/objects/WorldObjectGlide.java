package org.asf.emuferal.packets.xt.gameserver.objects;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class WorldObjectGlide implements IXtPacket<WorldObjectGlide> {

	private String playerUUID;

	@Override
	public WorldObjectGlide instantiate() {
		return new WorldObjectGlide();
	}

	@Override
	public String id() {
		return "og";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		writer.writeString(playerUUID);

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Glide
		Player plr = (Player) client.container;
		playerUUID = plr.account.getAccountID();
		plr.client.sendPacket(this);

		// Broadcast respawn
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				player.client.sendPacket(this);
			}
		}

		return true;
	}

}
