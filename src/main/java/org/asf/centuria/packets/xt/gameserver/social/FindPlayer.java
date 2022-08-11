package org.asf.centuria.packets.xt.gameserver.social;

import java.io.IOException;

import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class FindPlayer implements IXtPacket<FindPlayer> {

	private static final String PACKET_ID = "rffpu";
	
	private String name;
	private String accountId = "";
	private boolean success = false;

	@Override
	public FindPlayer instantiate() {
		return new FindPlayer();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		name = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // data prefix

		writer.writeBoolean(success); // success
		writer.writeString(accountId); // account ID

		writer.writeString(""); // data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Find avatar

		if (System.getProperty("debugMode") != null) {
			System.out.println("[SOCIAL] [FindPlayer] Client to server ( playerName: " + name + " )");
		}

		String id = AccountManager.getInstance().getUserByDisplayName(name);
		if (id == null || AccountManager.getInstance().getAccount(id).isBanned()) {
			client.sendPacket(this);

			// log interaction details
			if (System.getProperty("debugMode") != null) {
				System.out.println("[SOCIAL] [FindPlayer] Server to client ( " + this.build() + " )");
			}

			return true; // Account not found
		}

		// Send response

		this.accountId = id;
		this.success = true;
		client.sendPacket(this);

		if (System.getProperty("debugMode") != null) {
			System.out.println("[SOCIAL] [FindPlayer] Server to client ( " + this.build() + " )");
		}

		return true;
	}

}
