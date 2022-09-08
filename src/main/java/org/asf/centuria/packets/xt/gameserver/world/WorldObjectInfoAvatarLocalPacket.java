package org.asf.centuria.packets.xt.gameserver.world;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class WorldObjectInfoAvatarLocalPacket implements IXtPacket<WorldObjectInfoAvatarLocalPacket> {
	
	private static final String PACKET_ID = "oial";

	public double x;
	public double y;
	public double z;
	
	public double rx;
	public double ry;
	public double rz;
	public double rw;

	@Override
	public WorldObjectInfoAvatarLocalPacket instantiate() {
		return new WorldObjectInfoAvatarLocalPacket();
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

		// Position
		writer.writeDouble(x);
		writer.writeDouble(y);
		writer.writeDouble(z);

		// Rotation
		writer.writeDouble(rx);
		writer.writeDouble(ry);
		writer.writeDouble(rz);
		writer.writeDouble(rw);

		writer.writeString(DATA_SUFFIX); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return false;
	}

}
