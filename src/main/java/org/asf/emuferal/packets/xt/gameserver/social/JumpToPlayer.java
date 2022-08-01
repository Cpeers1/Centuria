package org.asf.emuferal.packets.xt.gameserver.social;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class JumpToPlayer implements IXtPacket<JumpToPlayer> {

	private static final String PACKET_ID = "rfjtr";

	public String accountID;
	
	public boolean success;
	public int roomIssId = -1;
	public String otherNode = "";

	@Override
	public JumpToPlayer instantiate() {
		return new JumpToPlayer();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		accountID = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // data prefix
		
		writer.writeBoolean(success); // success
		writer.writeInt(roomIssId); //roomIssId
		writer.writeString(otherNode); //other node?
		
		writer.writeString(""); // data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		Player player = ((Player) client.container);
		player.teleportToPlayer(accountID);
		return true; // Account not found, blocked or still loading
	}

}
