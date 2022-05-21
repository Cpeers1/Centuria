package org.asf.emuferal.packets.xt.gameserver.objects;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class WorldObjectUpdate implements IXtPacket<WorldObjectUpdate> {

	private int mode;
	private String data;

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
		mode = reader.readInt();
		data = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Object update
		Player plr = (Player) client.container;

		// Build broadcast packet
		XtReader rd = new XtReader(data);
		XtWriter pk = new XtWriter();
		pk.writeString("ou");
		pk.writeInt(-1); // Data prefix
		pk.writeString(plr.account.getAccountID());
		pk.writeInt(mode);
		pk.writeLong(System.currentTimeMillis() / 1000);
		switch (mode) {
		case 0:
		case 2: {
			double x = rd.readDouble();
			double y = rd.readDouble();
			double z = rd.readDouble();
			String d1 = rd.read();
			String d2 = rd.read();
			String d3 = rd.read();
			String d4 = rd.read();
			double rx = rd.readDouble();
			double ry = rd.readDouble();
			double rz = rd.readDouble();
			double rw = rd.readDouble();

			plr.lastRotW = rw;
			plr.lastRotX = rx;
			plr.lastRotY = ry;
			plr.lastRotZ = rz;
			plr.lastPosX = x;
			plr.lastPosY = y;
			plr.lastPosZ = z;

			pk.writeDouble(x);
			pk.writeDouble(y);
			pk.writeDouble(z);
			pk.writeString(d1);
			pk.writeDouble(rx);
			pk.writeDouble(ry);
			pk.writeDouble(rz);
			pk.writeString(d2);
			pk.writeString(d3);
			pk.writeString(d4);
			pk.writeDouble(rw);
			pk.writeInt(0);
			break;
		}
		case 4: {
			double x = rd.readDouble();
			double y = rd.readDouble();
			double z = rd.readDouble();
			String d1 = rd.read();
			String d2 = rd.read();
			String d3 = rd.read();
			String d4 = rd.read();
			double rx = rd.readDouble();
			double ry = rd.readDouble();
			double rz = rd.readDouble();
			double rw = rd.readDouble();
			int dd = rd.readInt();

			plr.lastRotW = rw;
			plr.lastRotX = rx;
			plr.lastRotY = ry;
			plr.lastRotZ = rz;
			plr.lastPosX = x;
			plr.lastPosY = y;
			plr.lastPosZ = z;

			pk.writeDouble(x);
			pk.writeDouble(y);
			pk.writeDouble(z);
			pk.writeString(d1);
			pk.writeDouble(rx);
			pk.writeDouble(ry);
			pk.writeDouble(rz);
			pk.writeString(d2);
			pk.writeString(d3);
			pk.writeString(d4);
			pk.writeDouble(rw);
			pk.writeInt(dd);
			break;
		}
		case 5: {
			return true; // FIXME: switch to the spawn finder when all spawns are implemented
		}
		default:
			mode = mode;
		}
		pk.writeString(""); // Data suffix

		// Save location
		plr.lastLocation = plr.lastPosX + "%" + plr.lastPosY + "%" + plr.lastPosZ + "%" + plr.lastRotX + "%"
				+ plr.lastRotY + "%" + plr.lastRotZ + "%" + plr.lastRotW;

		// Broadcast sync
		String msg = pk.encode();
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				try {
					player.client.sendPacket(msg);
				} catch (IOException e) {
				}
			}
		}

		// TODO
		return true;
	}

}
