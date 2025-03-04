package org.asf.centuria.tools.legacyclienttools;

import java.io.IOException;

import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.smartfox.ISmartfoxPacket;

public class XTPacketProxy implements ISmartfoxPacket {

	public static final int DATA_PREFIX = -1;
	public static final String DATA_SUFFIX = "";

	public String data;

	@Override
	public boolean canParse(String content) {
		if (!content.startsWith("%xt%"))
			return false;
		return true;
	}

	@Override
	public boolean parse(String content) throws IOException {
		if (!content.startsWith("%xt%"))
			return false;
		data = content;
		return true;
	}

	@Override
	public String build() {
		return data;
	}

	@Override
	public ISmartfoxPacket instantiate() {
		return new XTPacketProxy();
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		System.out.println("Proxy: " + data);
		((SmartfoxClient)client.container).sendPacket(this);
		return true;
	}

}
