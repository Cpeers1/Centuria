package org.asf.centuria.packets.xt.gameserver.trade;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class TradeInitiateCancelPacket implements IXtPacket<TradeInitiateCancelPacket> {

	private static final String PACKET_ID = "tic";
	
	@Override
	public TradeInitiateCancelPacket instantiate() {
		return new TradeInitiateCancelPacket();
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
			System.out.println("[TRADE] [TradeInitiateCancel] Client to server.");
		}
		
		Player player = ((Player) client.container);
		if(player.tradeEngagedIn != null)
		{
			player.tradeEngagedIn.cancelTrade();
		}
		return true;
	}

}
