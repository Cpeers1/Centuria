package org.asf.emuferal.packets.xt.gameserver.objects;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class WorldObjectSetRespawn implements IXtPacket<WorldObjectSetRespawn> {

	public double x;
	public double y;
	public double z;

	public double rw;
	public double rx;
	public double ry;
	public double rz;

	@Override
	public WorldObjectSetRespawn instantiate() {
		return new WorldObjectSetRespawn();
	}

	@Override
	public String id() {
		return "ors";
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
