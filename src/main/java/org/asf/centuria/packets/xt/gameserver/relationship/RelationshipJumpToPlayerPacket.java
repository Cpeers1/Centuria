package org.asf.centuria.packets.xt.gameserver.relationship;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class RelationshipJumpToPlayerPacket implements IXtPacket<RelationshipJumpToPlayerPacket> {

	private static final String PACKET_ID = "rfjtr";

	public String accountID;

	@Override
	public RelationshipJumpToPlayerPacket instantiate() {
		return new RelationshipJumpToPlayerPacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		accountID = reader.read();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		Player player = ((Player) client.container);
		player.teleportToPlayer(accountID);
		return true;
	}

}
