package org.asf.centuria.packets.xt.gameserver.world;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.players.Player;

public class JoinRoom implements IXtPacket<JoinRoom> {

	private static final String PACKET_ID = "rj";

	public int levelID = 0;
	public int levelType = 0;
	public int issRoomID = -1;
	public String roomIdentifier = "room_0";
	public String teleport = "";
	public boolean success = true;

	@Override
	public JoinRoom instantiate() {
		return new JoinRoom();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		levelID = reader.readInt();
		levelType = reader.readInt();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(issRoomID); // Iss Room ID (unused as we dont support it)

		writer.writeBoolean(success); // Success
		writer.writeInt(levelID); // Room ID
		writer.writeInt(levelType); // Room type
		writer.writeInt(issRoomID); // Iss Room ID (unused as we dont support it)
		writer.writeString(teleport); // Specific teleport
		writer.writeString(roomIdentifier); // Chat room ID

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		Player plr = (Player) client.container;
		plr.teleportToRoom(levelID, levelType, issRoomID, roomIdentifier, teleport);
		return true;
	}
	
	public JoinRoom markAsFailed()
	{
		this.success = false;
		this.roomIdentifier = "";
		this.levelID = 0;
		this.levelType = 0;
		this.teleport = "";
		this.issRoomID = -1;
		return this;
	}

}
