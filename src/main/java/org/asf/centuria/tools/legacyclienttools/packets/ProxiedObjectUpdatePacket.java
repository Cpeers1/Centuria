package org.asf.centuria.tools.legacyclienttools.packets;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.tools.legacyclienttools.servers.TranslatorGameServer;

public class ProxiedObjectUpdatePacket implements IXtPacket<ProxiedObjectUpdatePacket> {

	private String msg;

	@Override
	public ProxiedObjectUpdatePacket instantiate() {
		return new ProxiedObjectUpdatePacket();
	}

	@Override
	public String id() {
		return "ou";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		msg = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeString(msg);
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		XtReader rd = new XtReader(msg);
		TranslatorGameServer server = (TranslatorGameServer) client.getServer();
		if (server.isLocalClient(client)) {
			int mode = rd.readInt();
			rd.readLong(); // Timestamp

			// Coordinates
			double x = rd.readDouble();
			double y = rd.readDouble();
			double z = rd.readDouble();

			// Direction
			double dx = rd.readDouble();
			double dy = rd.readDouble();
			double dz = rd.readDouble();

			// Rotation
			double rx = rd.readDouble();
			double ry = rd.readDouble();
			double rz = rd.readDouble();
			double rw = rd.readDouble();

			// Speed
			float speed = rd.readFloat();

			// Write
			XtWriter wr = new XtWriter();
			wr.writeString("o");
			wr.writeString("ou");
			wr.writeInt(-1);
			wr.writeInt(mode);

			// Coordinates
			wr.writeDouble(x);
			wr.writeDouble(y);
			wr.writeDouble(z);

			// Direction
			wr.writeDouble(dx);
			wr.writeDouble(dy);
			wr.writeDouble(dz);

			// Rotation
			wr.writeDouble(rx);
			wr.writeDouble(ry);
			wr.writeDouble(rz);
			wr.writeDouble(rw);

			// Speed
			wr.writeFloat(speed);

			// Action
			if (mode == 4)
				wr.writeInt(rd.readInt());

			wr.writeString("");
			((SmartfoxClient) client.container).sendPacket(wr.encode());
			return true;
		} else {
			rd.read();

			// UUID
			String uuid = rd.read();

			// Mode
			int mode = rd.readInt();

			// Timestamp
			long time = rd.readLong();

			// Coordinates
			double x = rd.readDouble();
			double y = rd.readDouble();
			double z = rd.readDouble();

			// The mess
			double dx = rd.readDouble();
			double ry = rd.readDouble();
			double rz = rd.readDouble();
			double rw = rd.readDouble();
			double dy = rd.readDouble();
			double dz = rd.readDouble();
			double rx = rd.readDouble();
			float speed = rd.readFloat();

			// Action
			int action = rd.readInt();

			// Write
			XtWriter wr = new XtWriter();
			wr.writeString("ou");
			wr.writeInt(-1);
			wr.writeString(uuid);
			wr.writeInt(mode);
			wr.writeLong(time);

			// Coordinates
			wr.writeDouble(x);
			wr.writeDouble(y);
			wr.writeDouble(z);

			// Rotation
			wr.writeDouble(rx);
			wr.writeDouble(ry);
			wr.writeDouble(rz);
			wr.writeDouble(rw);

			// Direction
			wr.writeDouble(dx);
			wr.writeDouble(dy);
			wr.writeDouble(dz);

			// Speed
			wr.writeFloat(speed);

			// Action
			wr.writeInt(action);

			wr.writeString("");
			((SmartfoxClient) client.container).sendPacket(wr.encode());

			return true;
		}
	}

}
