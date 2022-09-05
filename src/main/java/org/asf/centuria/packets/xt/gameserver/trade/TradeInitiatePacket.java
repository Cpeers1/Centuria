package org.asf.centuria.packets.xt.gameserver.trade;

import java.io.IOException;

import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.entities.trading.Trade;
import org.asf.centuria.enums.trading.TradeValidationType;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class TradeInitiatePacket implements IXtPacket<TradeInitiatePacket> {

	private static final String PACKET_ID = "ti";
	
	//Inbound
	public String inboundUserId;
	
	//Outbound
	public TradeValidationType tradeValidationType = null;
	public String outboundUserId = null;
	public Boolean success = null;
	
	@Override
	public TradeInitiatePacket instantiate() {
		return new TradeInitiatePacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		inboundUserId = reader.read();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		if(tradeValidationType != null)
		{
			writer.writeInt(tradeValidationType.value); //trade validation type			
		}

		if(outboundUserId != null)
		{
			writer.writeString(outboundUserId); //user ID 			
		}

		if(success != null)
		{
			writer.writeBoolean(success); //success 		
		}
		
		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		
		if (System.getProperty("debugMode") != null) {
			System.out.println("[TRADE] [TradeInitiate] Client to server: ( inboundUserId: " + inboundUserId + " )");
		}
		
		Player sourcePlayer = ((Player) client.container);
		
		//Inbound user ID is the target player to trade
		Player targetPlayer = AccountManager.getInstance().getAccount(inboundUserId).getOnlinePlayerInstance();
		
		//Start a new trade.
		var trade = Trade.startNewTrade(sourcePlayer, targetPlayer);
		
		return true;
	}

}
