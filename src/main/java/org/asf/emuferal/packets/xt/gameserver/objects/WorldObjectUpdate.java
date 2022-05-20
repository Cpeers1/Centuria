package org.asf.emuferal.packets.xt.gameserver.objects;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class WorldObjectUpdate implements IXtPacket<WorldObjectUpdate> {

	private String target;
	private String start;

	public double x;
	public double y;
	public double z;

	public double d1;
	public double d2;
	public double d3;
	public double d4;

	public double rw;
	public double rx;
	public double ry;
	public double rz;

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
		start = reader.read();
		x = reader.readDouble();
		y = reader.readDouble();
		z = reader.readDouble();
		d1 = reader.readDouble();
		d2 = reader.readDouble();
		d3 = reader.readDouble();
		d4 = reader.readDouble();
		rw = reader.readDouble();
		rx = reader.readDouble();
		ry = reader.readDouble();
		rz = reader.readDouble();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data suffix

		writer.writeString(target);
		writer.add(start);
		writer.add(Long.toString(System.currentTimeMillis() / 1000));
		writer.writeDouble(x);
		writer.writeDouble(y);
		writer.writeDouble(z);
		writer.writeDouble(rw);
		writer.writeDouble(rx);
		writer.writeDouble(ry);
		writer.writeDouble(rz);
		writer.writeDouble(d1);
		writer.writeDouble(d2);
		writer.writeDouble(d3);
		writer.writeDouble(d4);
		writer.writeString("0");

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Object update
		Player plr = (Player) client.container;
		target = plr.account.getAccountID();

		// Broadcast sync
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				try {
					player.client.sendPacket(this);
				} catch (IOException e) {
				}
			}
		}

		// TODO
		return true;
	}

}
