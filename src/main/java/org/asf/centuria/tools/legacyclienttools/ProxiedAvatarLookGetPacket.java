package org.asf.centuria.tools.legacyclienttools;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class ProxiedAvatarLookGetPacket implements IXtPacket<ProxiedAvatarLookGetPacket> {

	private String msg;

	@Override
	public ProxiedAvatarLookGetPacket instantiate() {
		return new ProxiedAvatarLookGetPacket();
	}

	@Override
	public String id() {
		return "alg";
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
		return true;
	}

}
