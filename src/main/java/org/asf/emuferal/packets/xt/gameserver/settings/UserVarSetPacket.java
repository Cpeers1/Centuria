package org.asf.emuferal.packets.xt.gameserver.settings;

import java.io.IOException;

import org.asf.emuferal.accounts.highlevel.PlayerSettingsAccessor;
import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.interactions.NetworkedObjects;
import org.asf.emuferal.interactions.dataobjects.NetworkedObject;
import org.asf.emuferal.modules.eventbus.EventBus;
import org.asf.emuferal.modules.events.interactions.InteractionStartEvent;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class UserVarSetPacket implements IXtPacket<UserVarSetPacket> {

	private int varDefId;
	private int value1;
	private int value2;
	private int value3;

	@Override
	public UserVarSetPacket instantiate() {
		return new UserVarSetPacket();
	}

	@Override
	public String id() {
		return "zs";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		varDefId = reader.readInt();
		value1 = reader.readInt();
		value2 = reader.readInt();
		value3 = reader.readInt();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		// TODO: verify this
		writer.writeInt(-1); // Data prefix

		writer.writeInt(varDefId);
		writer.writeInt(value1);
		writer.writeInt(value2);
		writer.writeInt(value3);

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		var player = (Player) client.container;
		
		var varAccessor = new PlayerSettingsAccessor(player.account.getPlayerInventory());
		boolean success = varAccessor.setPlayerVars(varDefId, new int[] { value1 });
		
		// log interaction details
		if (System.getProperty("debugMode") != null) {
			System.out.println("[SETTINGS] [USERVARSET]  Client to server (varDefId: " + varDefId + ", value1: " + value1 + ", value2: " + value2 + ", value3: " + value3 + " )");
		}
		
		//build a response
		
		XtWriter wr = new XtWriter();
		wr.writeString("zs");
		wr.writeInt(-1); // data prefix
		wr.writeBoolean(success);
		wr.writeInt(varDefId);
		wr.writeInt(value1);
		wr.writeInt(0); //todo: multiindex user vars
		wr.writeString(""); // data suffix
		
		if (System.getProperty("debugMode") != null) {
			System.out.println("[SETTINGS] [USERVARSET]  Sending Response: " + wr.encode() +" ... ");
		}
				
		client.sendPacket(wr.encode());

		return true;
	}

}
