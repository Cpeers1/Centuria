package org.asf.centuria.packets.xt.gameserver.trading;

import java.io.IOException;
import java.util.List;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.inventoryitems.InventoryItem;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.enums.trading.TradeValidationType;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class TradeListPacket implements IXtPacket<TradeListPacket> {

	private static final String PACKET_ID = "tl";
	
	// Inbound
	private String targetPlayerId;
	
	// Outbound
	public List<InventoryItem> tradeListItems;
	
	@Override
	public TradeListPacket instantiate() {
		return new TradeListPacket();
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
		writer.writeInt(-1); // Data prefix

		writer.writeInt(tradeValidationType.value); //trade validation type
		writer.writeString(userId); //user ID (For the tradee?)
		writer.writeBoolean(success); //success 
		
		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return true;
	}

}
