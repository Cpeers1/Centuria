package org.asf.centuria.packets.xt.gameserver.setting;

import java.io.IOException;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.highlevel.impl.UserVarAccessorImpl;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;

import com.google.gson.JsonArray;

public class SettingsSetPacket implements IXtPacket<SettingsSetPacket> {

	private static final String PACKET_ID = "zs";

	private int varDefId;
	private int value;
	private int index;

	@Override
	public SettingsSetPacket instantiate() {
		return new SettingsSetPacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		varDefId = reader.readInt();
		value = reader.readInt();
		index = reader.readInt();
	}

	@Override
	public void build(XtWriter wr) throws IOException {
		wr.writeInt(DATA_PREFIX); // Data prefix

		wr.writeBoolean(true);
		wr.writeInt(varDefId);
		wr.writeInt(value);
		wr.writeInt(index);

		wr.writeString(DATA_SUFFIX); // data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// log interaction details
		if (Centuria.debugMode) {
			System.out.println("[SETTINGS] [USERVARSET]  Client to server (varDefId: " + varDefId + ", value: " + value
					+ ", index: " + index + ")");
		}

		var player = (Player) client.container;

		var varAccessor = new UserVarAccessorImpl(player.account.getSaveSpecificInventory());
		var output = varAccessor.setPlayerVarValue(varDefId, index, value);

		var outputInv = new JsonArray();

		for (var item : output.changedUserVars) {
			outputInv.add(item.toJsonObject());
		}

		if (Centuria.debugMode) {
			System.out.println("[SETTINGS] [USERVARSET] output inv: " + outputInv.toString());
		}

		// send changed var inventory...
		var itemPacket = new InventoryItemPacket();
		itemPacket.item = outputInv;

		// send packet..
		client.sendPacket(itemPacket);

		if (Centuria.debugMode) {
			System.out.println("[SETTINGS] [USERVARSET]  Sending Response: " + itemPacket.build() + " ... ");
		}

		client.sendPacket(this);

		if (Centuria.debugMode) {
			System.out.println("[SETTINGS] [USERVARSET]  Sending Response: " + this.build() + " ... ");
		}

		return true;
	}

}
