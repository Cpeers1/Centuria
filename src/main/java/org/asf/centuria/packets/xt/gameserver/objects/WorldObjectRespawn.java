package org.asf.centuria.packets.xt.gameserver.objects;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class WorldObjectRespawn implements IXtPacket<WorldObjectRespawn> {

	private static final String PACKET_ID = "orr";

	private String data;
	private String playerUUID;

	@Override
	public WorldObjectRespawn instantiate() {
		return new WorldObjectRespawn();
	}

	@Override
	public String id() {
		return PACKET_ID;
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
		plr.client.sendPacket(this);

		XtWriter pk = new XtWriter();
		pk.writeString("ou");
		pk.writeInt(-1); // Data prefix
		pk.writeString(playerUUID);
		pk.add("0");
		pk.add(Long.toString(System.currentTimeMillis() / 1000));
		pk.writeString(data);
		pk.writeString("0%0%0%0");
		pk.writeString("0");
		pk.writeString(""); // Data suffix

		// Log if in debug
		if (System.getProperty("debugMode") != null)
			System.out.println("Respawn set: " + plr.account.getDisplayName() + ": " + plr.respawn.replace("%", ", "));

		// Broadcast respawn
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				player.client.sendPacket(pk.encode());
			}
		}

		return true;
	}

}
