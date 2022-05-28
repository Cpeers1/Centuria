package org.asf.emuferal.packets.xt.gameserver.social;

import java.io.IOException;

import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;

public class FindPlayer implements IXtPacket<FindPlayer> {

	private String name;

	@Override
	public FindPlayer instantiate() {
		return new FindPlayer();
	}

	@Override
	public String id() {
		return "rffpu";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		name = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Find avatar
		
		//log interaction details
		if (System.getProperty("debugMode") != null) {
			System.out.println("[SOCIAL] [FindPlayer] Client to server ( playerName: " + name + " )");
		}
		
		String id = AccountManager.getInstance().getUserByDisplayName(name);
		if (id == null) {
			XtWriter writer = new XtWriter();
			writer.writeString("rffpu");
			writer.writeInt(-1); // data prefix
			writer.writeBoolean(false); // success
			writer.writeString(""); // account ID
			writer.writeString(""); // data suffix
			client.sendPacket(writer.encode());
			
			//log interaction details
			if (System.getProperty("debugMode") != null) {
				System.out.println("[SOCIAL] [FindPlayer] Server to client ( success: false, accountId: )");
			}
			
			return true; // Account not found
		}

		// Send response
		XtWriter writer = new XtWriter();
		writer.writeString("rffpu");
		writer.writeInt(-1); // data prefix
		writer.writeBoolean(true); // success
		writer.writeString(AccountManager.getInstance().getAccount(id).getAccountID()); // account ID
		writer.writeString(""); // data suffix
		client.sendPacket(writer.encode());
		
		//log interaction details
		if (System.getProperty("debugMode") != null) {
			System.out.println("[SOCIAL] [FindPlayer] Server to client ( success: true, accountId: " + AccountManager.getInstance().getAccount(id).getAccountID() + " )");
		}

		return true;
	}

}
