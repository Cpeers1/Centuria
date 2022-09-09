package org.asf.centuria.packets.xt.gameserver.trade;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class TradeReadyPacket implements IXtPacket<TradeReadyPacket> {

	private static final String PACKET_ID = "tr";
	
	//inbound
	public int inbound_ReadyState;
	
	//outbound
	public boolean outbound_Success;
	public String outbound_UserId;
	public int outbound_ReadyState;
	
	@Override
	public TradeReadyPacket instantiate() {
		return new TradeReadyPacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		inbound_ReadyState = reader.readInt();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(DATA_PREFIX); // Data prefix
		
		writer.writeBoolean(outbound_Success);
		writer.writeString(outbound_UserId);
		writer.writeInt(outbound_ReadyState);
		
		writer.writeString(DATA_SUFFIX); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		
		if (System.getProperty("debugMode") != null) {
			System.out.println("[TRADE] [TradeReady] Client to server: ( readyState: " + inbound_ReadyState + " )");
		}
		
		Player player = ((Player) client.container);
		if(player.tradeEngagedIn != null)
		{
			player.tradeEngagedIn.tradeReady(player, inbound_ReadyState);
		}
		return true;
	}

}
