package org.asf.centuria.tools.legacyclienttools;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class ProxiedRoomJoinPacket implements IXtPacket<ProxiedRoomJoinPacket> {

	private String msg;

	@Override
	public ProxiedRoomJoinPacket instantiate() {
		return new ProxiedRoomJoinPacket();
	}

	@Override
	public String id() {
		return "rj";
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
		rd.read();
		boolean success = rd.readBoolean();
		if (success) {
			String defID = rd.read();
			rd.readInt(); // Level type
			int issRoom = rd.readInt();
			String uuid = rd.read();
			String converstaion = rd.read();
			
			XtWriter wr = new XtWriter();
			wr.writeString("rj");
			wr.writeInt(-1);
			wr.writeBoolean(success);
			wr.writeString(defID);
			wr.writeInt(issRoom);
			wr.writeString(uuid);
			wr.writeString(converstaion);
			wr.writeString("");
			((SmartfoxClient)client.container).sendPacket(wr.encode());
			
			return true;
		} else
			return false;
	}

}
