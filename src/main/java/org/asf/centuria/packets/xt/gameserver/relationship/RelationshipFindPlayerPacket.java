package org.asf.centuria.packets.xt.gameserver.relationshipss;

import java.io.IOException;

import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class RelationshipFindPlayerPacket implements IXtPacket<RelationshipFindPlayerPacket> {

	private static final String PACKET_ID = "rffpu";
	
	private String name;
	private String accountId = "";
	private boolean success = false;

	@Override
	public RelationshipFindPlayerPacket instantiate() {
		return new RelationshipFindPlayerPacket();
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
		writer.writeInt(DATA_PREFIX); // data prefix

		writer.writeBoolean(success); // success
		writer.writeString(accountId); // account ID

		writer.writeString(DATA_SUFFIX); // data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Find avatar

		if (Centuria.debugMode) {
			System.out.println("[SOCIAL] [FindPlayer] Client to server ( playerName: " + name + " )");
		}

		String id = AccountManager.getInstance().getUserByDisplayName(name);
		if (id == null || AccountManager.getInstance().getAccount(id).isBanned()) {
			client.sendPacket(this);

			// log interaction details
			if (Centuria.debugMode) {
				System.out.println("[SOCIAL] [FindPlayer] Server to client ( " + this.build() + " )");
			}

			return true; // Account not found
		}

		// Send response

		this.accountId = id;
		this.success = true;
		client.sendPacket(this);

		if (Centuria.debugMode) {
			System.out.println("[SOCIAL] [FindPlayer] Server to client ( " + this.build() + " )");
		}

		return true;
	}

}
