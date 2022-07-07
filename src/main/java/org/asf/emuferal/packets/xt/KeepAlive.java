package org.asf.emuferal.packets.xt;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;

public class KeepAlive implements IXtPacket<KeepAlive> {

	private static final String PACKET_ID = "ka";

	@Override
	public KeepAlive instantiate() {
		return new KeepAlive();
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
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return true;
	}

}
