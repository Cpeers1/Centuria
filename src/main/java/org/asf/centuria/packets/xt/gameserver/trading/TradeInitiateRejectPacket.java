package org.asf.centuria.packets.xt.gameserver.trading;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class TradeInitiateRejectPacket implements IXtPacket<TradeInitiateRejectPacket> {

	private static final String PACKET_ID = "tir";
	
	@Override
	public TradeInitiateRejectPacket instantiate() {
		return new TradeInitiateRejectPacket();
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
			System.out.println("[TRADE] [TradeInitiateReject] Client to server.");
		}
		
		Player player = ((Player) client.container);
		if(player.tradeEngagedIn != null)
		{
			player.tradeEngagedIn.rejectTrade();
		}
		return true;
	}

}
