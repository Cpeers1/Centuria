package org.asf.centuria.packets.xt.gameserver.social;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.enums.players.OnlineStatus;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class RelationshipFollowOnlineStatusUpdate implements IXtPacket<RelationshipFollowOnlineStatusUpdate> {

	private static final String PACKET_ID = "rfosu";

	public String userUUID = "";
	public OnlineStatus playerOnlineStatus;

	@Override
	public RelationshipFollowOnlineStatusUpdate instantiate() {
		return new RelationshipFollowOnlineStatusUpdate();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(DATA_PREFIX); // data prefix
		
		writer.writeString(userUUID);
		writer.writeInt(playerOnlineStatus.value);
		
		writer.writeString(DATA_SUFFIX); // data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		return true;
	}

}
