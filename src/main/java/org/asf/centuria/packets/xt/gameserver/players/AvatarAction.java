package org.asf.centuria.packets.xt.gameserver.players;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.enums.actors.ActorActionType;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.players.Player;

public class AvatarAction implements IXtPacket<AvatarAction> {

	private static final String PACKET_ID = "aa";

	private String action;

	@Override
	public AvatarAction instantiate() {
		return new AvatarAction();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		action = reader.read();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {

		//Set action
		Player plr = (Player) client.container;
		plr.lastAction = ActorActionType.getAvatarActionType(action);

		//Write packet
		XtWriter pk = new XtWriter();
		pk.writeString("ou");
		pk.writeInt(-1); // Data prefix
		pk.writeString(plr.account.getAccountID());
		pk.writeInt(4);
		pk.writeLong(System.currentTimeMillis() / 1000);
		pk.writeDouble(plr.lastPosX);
		pk.writeDouble(plr.lastPosY);
		pk.writeDouble(plr.lastPosZ);
		pk.writeString("0");
		pk.writeDouble(plr.lastRotX);
		pk.writeDouble(plr.lastRotY);
		pk.writeDouble(plr.lastRotZ);
		pk.writeString("0");
		pk.writeString("0");
		pk.writeString("0");
		pk.writeDouble(plr.lastRotW);
		pk.writeInt(plr.lastAction.value);
		pk.writeString(""); // Data suffix

		// Broadcast sync
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				player.client.sendPacket(pk.encode());
			}
		}
		

		return true;
	}

}
