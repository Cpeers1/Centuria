package org.asf.centuria.packets.xt.gameserver.objects;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class WorldObjectGlide implements IXtPacket<WorldObjectGlide> {

	private static final String PACKET_ID = "og";

	private String playerUUID;

	@Override
	public WorldObjectGlide instantiate() {
		return new WorldObjectGlide();
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
		writer.writeInt(DATA_PREFIX); // Data prefix

		writer.writeString(playerUUID);

		writer.writeString(DATA_SUFFIX); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Glide
		Player plr = (Player) client.container;
		playerUUID = plr.account.getAccountID();
		plr.client.sendPacket(this);

		// Broadcast packet
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				player.client.sendPacket(this);
			}
		}

		return true;
	}

}
