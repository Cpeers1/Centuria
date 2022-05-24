package org.asf.emuferal.packets.xt.gameserver.world;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;

public class WorldObjectInfoAvatarLocal implements IXtPacket<WorldObjectInfoAvatarLocal> {
	
	public double x;
	public double y;
	public double z;
	
	public double rx;
	public double ry;
	public double rz;
	public double rw;

	@Override
	public WorldObjectInfoAvatarLocal instantiate() {
		return new WorldObjectInfoAvatarLocal();
	}

	@Override
	public String id() {
		return "oial";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		// Position
		writer.writeDouble(x);
		writer.writeDouble(y);
		writer.writeDouble(z);

		// Rotation
		writer.writeDouble(rx);
		writer.writeDouble(ry);
		writer.writeDouble(rz);
		writer.writeDouble(rw);

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return false;
	}

}
