package org.asf.centuria.tools.legacyclienttools;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ProxiedObjectInfoPacket implements IXtPacket<ProxiedObjectInfoPacket> {

	private String msg;

	@Override
	public ProxiedObjectInfoPacket instantiate() {
		return new ProxiedObjectInfoPacket();
	}

	@Override
	public String id() {
		return "oi";
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
		if (!server.isLocalClient(client)) {
			rd.read();

			// UUID
			String uuid = rd.read();
			int defID = rd.readInt();
			String owner = rd.read();

			// Mode
			int mode = rd.readInt();

			// Timestamp
			long time = rd.readLong();

			// Coordinates
			double x = rd.readDouble();
			double y = rd.readDouble();
			double z = rd.readDouble();

			// Rotation
			double rx = rd.readDouble();
			double ry = rd.readDouble();
			double rz = rd.readDouble();
			double rw = rd.readDouble();

			// Direction
			double dx = rd.readDouble();
			double dy = rd.readDouble();
			double dz = rd.readDouble();
			float speed = rd.readFloat();

			// Action
			int action = rd.readInt();

			// Look
			String look = rd.read();
			String name = rd.read();
			int unk = rd.readInt();

			// Check avatar
			if (defID == 852) {
				// Avatar
				JsonObject avaInfo = JsonParser.parseString(look).getAsJsonObject();
				avaInfo = avaInfo;
				// TODO: convert look data to beta-compatible info
			}

			// Write basics
			XtWriter wr = new XtWriter();
			wr.writeString("oi");
			wr.writeInt(-1);
			wr.writeString(uuid);
			wr.writeInt(defID);
			wr.writeString(owner);
			wr.writeInt(mode);
			wr.writeLong(time);

			// Coordinates
			wr.writeDouble(x);
			wr.writeDouble(y);
			wr.writeDouble(z);

			// Rotation
			wr.writeDouble(rx);
			wr.writeDouble(ry);
			wr.writeDouble(rz - 180);
			wr.writeDouble(rw);

			// Direction
			wr.writeDouble(dx);
			wr.writeDouble(dy);
			wr.writeDouble(dz);

			// Speed
			wr.writeFloat(speed);

			// Action
			wr.writeInt(action);

			// Data
			wr.writeString(look);
			wr.writeString(name);
			wr.writeInt(unk);

			wr.writeString("");
			((SmartfoxClient) client.container).sendPacket(wr.encode());

			return true;
		}
		return false;
	}

}
