package org.asf.centuria.packets.xt.gameserver.world;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class WorldObjectRespawnSetPacket implements IXtPacket<WorldObjectRespawnSetPacket> {

	private static final String PACKET_ID = "ors";

	public double x;
	public double y;
	public double z;

	public double rw;
	public double rx;
	public double ry;
	public double rz;

	@Override
	public WorldObjectRespawnSetPacket instantiate() {
		return new WorldObjectRespawnSetPacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		x = reader.readDouble();
		y = reader.readDouble();
		z = reader.readDouble();
		rw = reader.readDouble();
		rx = reader.readDouble();
		ry = reader.readDouble();
		rz = reader.readDouble();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Set respawn
		Player plr = (Player) client.container;
		plr.respawn = x + "%" + y + "%" + z + "%" + rw + "%" + rx + "%" + ry + "%" + rz;
		return true;
	}

}
