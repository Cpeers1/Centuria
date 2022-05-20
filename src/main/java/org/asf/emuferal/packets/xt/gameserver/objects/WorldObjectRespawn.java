package org.asf.emuferal.packets.xt.gameserver.objects;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class WorldObjectRespawn implements IXtPacket<WorldObjectRespawn> {

	private String data;
	private String playerUUID;

	@Override
	public WorldObjectRespawn instantiate() {
		return new WorldObjectRespawn();
	}

	@Override
	public String id() {
		return "orr";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		writer.writeString(playerUUID);
		writer.writeString(data);

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Object respawn
		Player plr = (Player) client.container;
		data = plr.respawn;
		playerUUID = plr.account.getAccountID();

		// Broadcast respawn
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (player.room.equals(plr.room)) {
				player.client.sendPacket(this);
			}
		}

		return true;
	}

}
