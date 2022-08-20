package org.asf.centuria.packets.xt.gameserver.trading;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.enums.trading.TradeValidationType;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class TradeInitateRejectPacket implements IXtPacket<TradeInitateRejectPacket> {

	private static final String PACKET_ID = "ti";
	
	@Override
	public TradeInitateRejectPacket instantiate() {
		return new TradeInitateRejectPacket();
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
