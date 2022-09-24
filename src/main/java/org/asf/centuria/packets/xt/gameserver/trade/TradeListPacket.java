package org.asf.centuria.packets.xt.gameserver.trade;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.inventoryitems.InventoryItem;
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
		writer.writeInt(DATA_PREFIX); // Data prefix

		JsonArray items = new JsonArray();
		for (var item : tradeListItems) {
			items.add(item.toJsonObject());
		}

		ByteArrayOutputStream op = new ByteArrayOutputStream();
		GZIPOutputStream gz = new GZIPOutputStream(op);
		gz.write(items.toString().getBytes("UTF-8"));
		gz.close();
		op.close();
		writer.writeString(Base64.getEncoder().encodeToString(op.toByteArray()));

		writer.writeString(DATA_SUFFIX); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {

		if (System.getProperty("debugMode") != null) {
			System.out.println("[TRADE] [TradeList] Client to server: ( playerId: " + targetPlayerId + " )");
		}

		GameServer srv = (GameServer) client.getServer();
		var targetPlayer = srv.getPlayer(targetPlayerId);
		if (targetPlayer == null) {
			// Offline so do it manually
			try {
				tradeListItems = AccountManager.getInstance().getAccount(targetPlayerId).getPlayerInventory()
						.getItemAccessor(targetPlayer).loadTradeList();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				tradeListItems = new ArrayList<InventoryItem>();
			}
			client.sendPacket(this);
			return true;
		}
		tradeListItems = targetPlayer.getTradeList();

		client.sendPacket(this);

		if (System.getProperty("debugMode") != null) {
			System.out.println("[TRADE] [TradeList] Server to client: " + this.build());
		}

		return true;
	}

}
