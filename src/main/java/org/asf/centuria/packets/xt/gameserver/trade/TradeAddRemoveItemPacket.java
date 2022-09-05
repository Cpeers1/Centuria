package org.asf.centuria.packets.xt.gameserver.trade;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class TradeAddRemoveItemPacket implements IXtPacket<TradeAddRemoveItemPacket> {

	private static final String PACKET_ID = "tar";

	// Inbound
	public int inboundIsAdding;
	public String inboundItemInvId;
	public int inboundQuantity;

	// Outbound
	public boolean success;
	public String userId;
	public int isAdding;
	public JsonObject updatedItem;
	public int quantity;

	@Override
	public TradeAddRemoveItemPacket instantiate() {
		return new TradeAddRemoveItemPacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		inboundIsAdding = reader.readInt();
		inboundItemInvId = reader.read();
		inboundQuantity = reader.readInt();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		writer.writeBoolean(success);

		if (success) {
			writer.writeString(userId);
			writer.writeInt(isAdding);
			writer.writeInt(quantity);

			ByteArrayOutputStream op = new ByteArrayOutputStream();
			GZIPOutputStream gz = new GZIPOutputStream(op);
			gz.write(updatedItem.toString().getBytes("UTF-8"));
			gz.close();
			op.close();
			writer.writeString(Base64.getEncoder().encodeToString(op.toByteArray()));
		}

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {

		if (System.getProperty("debugMode") != null) {
			System.out
					.println("[TRADE] [TradeAddRemoveItemPacket] Client to server: (inboundIsAdding: " + inboundIsAdding
							+ ", inboundItemInvId: " + inboundItemInvId + ", inboundQuantity: " + inboundQuantity);
		}

		Player player = ((Player) client.container);
		if (player.tradeEngagedIn != null) {
			if (inboundIsAdding > 0) {
				var accessor = player.account.getPlayerInventory().getAccessor();
				var item = accessor.findInventoryObject(accessor.getInventoryIDOfItem(inboundItemInvId),
						inboundItemInvId);
				player.tradeEngagedIn.addItemToTrade(player, inboundItemInvId, item, inboundQuantity);
			} else {
				player.tradeEngagedIn.removeItemFromTrade(player, inboundItemInvId, inboundQuantity);
			}
		}
		return true;
	}

}