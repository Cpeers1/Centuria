package org.asf.emuferal.packets.xt.gameserver.settings;

import java.io.IOException;

import org.asf.emuferal.accounts.highlevel.impl.UserVarAccessorImpl;
import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.interactions.NetworkedObjects;
import org.asf.emuferal.interactions.dataobjects.NetworkedObject;
import org.asf.emuferal.modules.eventbus.EventBus;
import org.asf.emuferal.modules.events.interactions.InteractionStartEvent;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.emuferal.players.Player;

public class UserVarSetPacket implements IXtPacket<UserVarSetPacket> {

	private int varDefId;
	private int value;
	private int value2; //TODO: what is this really?
	private int value3; //TODO: what is this really?
	private boolean success;

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
		value = reader.readInt();
		value2 = reader.readInt();
		value3 = reader.readInt();
	}

	@Override
	public void build(XtWriter wr) throws IOException {
		// TODO: verify this
		wr.writeInt(-1); // Data prefix

		wr.writeBoolean(success);
		wr.writeInt(varDefId);
		wr.writeInt(value);
		wr.writeInt(0); //TODO: multiindex user vars
		
		wr.writeString(""); // data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		
		// log interaction details
		if (System.getProperty("debugMode") != null) {
			System.out.println("[SETTINGS] [USERVARSET]  Client to server (varDefId: " + varDefId + ", value1: " + value + ", value2: " + value2 + ", value3: " + value3 + " )");
		}
		
		var player = (Player) client.container;
		
		//var varAccessor = new UserVarAccessorImpl(player.account.getPlayerInventory());
		//var output = varAccessor.setPlayerVars(varDefId, new int[] { value });
		
		//success = output.success;
		//var outputInv = output.changedVarInv;	
		
		if (System.getProperty("debugMode") != null) {
			//System.out.println("[SETTINGS] [USERVARSET] output inv: " + outputInv.toString());
		}
		
		//send changed var inventory...	
		var itemPacket = new InventoryItemPacket();
		//itemPacket.item = outputInv;
		
		XtWriter wr = new XtWriter();
		itemPacket.build(wr);
		
		//send packet..
		client.sendPacket(wr.encode());
		
		if (System.getProperty("debugMode") != null) {
			System.out.println("[SETTINGS] [USERVARSET]  Sending Response: " + wr.encode() + " ... ");
		}			
		
		//build a response		
		wr = new XtWriter();
		build(wr);

		if (System.getProperty("debugMode") != null) {
			System.out.println("[SETTINGS] [USERVARSET]  Sending Response: " + wr.encode() + " ... ");
		}
				
		client.sendPacket(wr.encode());
		

		return true;
	}

}
