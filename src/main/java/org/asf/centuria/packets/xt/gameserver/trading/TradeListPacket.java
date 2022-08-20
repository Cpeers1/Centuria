package org.asf.centuria.packets.xt.gameserver.trading;

import java.io.IOException;
import java.util.List;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.inventoryitems.InventoryItem;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.enums.trading.TradeValidationType;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

import com.google.gson.JsonArray;

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
		targetPlayerId = reader.read();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		JsonArray items = new JsonArray();
		for(var item : tradeListItems)
		{
			items.add(item.toJsonObject());
		}
		writer.writeString(items.toString());; //trade validation type
		
		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		GameServer srv = (GameServer) client.getServer();
		var targetPlayer = srv.getPlayer(targetPlayerId);
		tradeListItems = targetPlayer.getTradeList();
		
		client.sendPacket(this);	
		return true;
	}

}
