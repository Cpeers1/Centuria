package org.asf.centuria.packets.xt.gameserver.social;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

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
		writer.writeInt(DATA_PREFIX); // data prefix
		
		writer.writeBoolean(success); // success
		writer.writeInt(roomIssId); //roomIssId
		writer.writeString(otherNode); //other node?
		
		writer.writeString(DATA_SUFFIX); // data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		Player player = ((Player) client.container);
		player.teleportToPlayer(accountID);
		return true; // Account not found, blocked or still loading
	}

}
