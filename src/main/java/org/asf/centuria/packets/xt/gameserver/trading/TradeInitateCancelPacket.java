package org.asf.centuria.packets.xt.gameserver.trading;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class TradeInitateCancelPacket implements IXtPacket<TradeInitateCancelPacket> {

	private static final String PACKET_ID = "tic";
	
	@Override
	public TradeInitateCancelPacket instantiate() {
		return new TradeInitateCancelPacket();
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
