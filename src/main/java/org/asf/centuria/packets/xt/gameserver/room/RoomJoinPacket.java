package org.asf.centuria.packets.xt.gameserver.room;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class RoomJoinPacket implements IXtPacket<RoomJoinPacket> {

	private static final String PACKET_ID = "rj";

	public int levelID = 0;
	public int levelType = 0;
	public int issRoomID = -1;
	public String roomIdentifier = "room_0";
	public String teleport = "";
	public boolean success = true;

	@Override
	public RoomJoinPacket instantiate() {
		return new RoomJoinPacket();
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
		writer.writeInt(DATA_PREFIX); // Iss Room ID (unused as we dont support it)

		writer.writeBoolean(success); // Success
		writer.writeInt(levelID); // Room ID
		writer.writeInt(levelType); // Room type
		writer.writeInt(issRoomID); // Iss Room ID (unused as we dont support it)
		writer.writeString(teleport); // Specific teleport??
		writer.writeString(roomIdentifier); // Chat room ID

		writer.writeString(DATA_SUFFIX); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		Player plr = (Player) client.container;
		roomIdentifier = "room_" + levelID;
		plr.teleportToRoom(levelID, levelType, issRoomID, roomIdentifier, teleport);
		return true;
	}

	public RoomJoinPacket markAsFailed() {
		this.success = false;
		this.roomIdentifier = "";
		this.levelID = 0;
		this.levelType = 0;
		this.teleport = "";
		this.issRoomID = -1;
		return this;
	}

}
