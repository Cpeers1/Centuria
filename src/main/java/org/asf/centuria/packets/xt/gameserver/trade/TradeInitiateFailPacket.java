package org.asf.centuria.packets.xt.gameserver.trade;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class TradeInitiateFailPacket implements IXtPacket<TradeInitiateFailPacket> {

	private static final String PACKET_ID = "tic"; // TODO: correct this packet
	
	@Override
	public TradeInitiateFailPacket instantiate() {
		return new TradeInitiateFailPacket();
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
		writer.writeString(DATA_SUFFIX); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		
		if (System.getProperty("debugMode") != null) {
			System.out.println("[TRADE] [TradeInitateFail] Client to server.");
		}
		
		return true;
	}

}
