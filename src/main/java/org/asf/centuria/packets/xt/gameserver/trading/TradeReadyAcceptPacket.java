package org.asf.centuria.packets.xt.gameserver.trading;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class TradeReadyAcceptPacket implements IXtPacket<TradeReadyAcceptPacket> {

	private static final String PACKET_ID = "tra";
	
	public boolean outbound_Success;
	public boolean outbound_WaitingForOtherPlayer;
	public Map<String, String> outbound_AddedItems = new HashMap<String, String>();
	
	
	@Override
	public TradeReadyAcceptPacket instantiate() {
		return new TradeReadyAcceptPacket();
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
		
		writer.writeBoolean(outbound_Success);
		
		if(outbound_Success)
		{
			writer.writeInt(outbound_WaitingForOtherPlayer ? 1 : 0);
			
			if(!outbound_WaitingForOtherPlayer)
			{
				writer.writeInt(outbound_AddedItems.size());
				for(var entry: outbound_AddedItems.entrySet())
				{
					writer.writeString(entry.getKey());
					writer.writeString(entry.getValue());
				}					
			}
		}
		
		writer.writeString(DATA_SUFFIX); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		
		if (System.getProperty("debugMode") != null) {
			System.out.println("[TRADE] [TradeReadyAccept] Client to server.");
		}
		
		Player player = ((Player) client.container);
		if(player.tradeEngagedIn != null)
		{
			player.tradeEngagedIn.TradeReadyAccept(player);
		}
		return true;
	}

}