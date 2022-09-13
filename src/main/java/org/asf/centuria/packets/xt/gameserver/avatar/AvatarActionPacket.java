package org.asf.centuria.packets.xt.gameserver.avatar;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class AvatarActionPacket implements IXtPacket<AvatarActionPacket> {

	private static final String PACKET_ID = "aa";

	private String action;

	@Override
	public AvatarActionPacket instantiate() {
		return new AvatarActionPacket();
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
		// Avatar action
		Player plr = (Player) client.container;

		XtWriter pk = new XtWriter();
		pk.writeString("ou");
		pk.writeInt(DATA_PREFIX); // Data prefix
		pk.writeString(plr.account.getAccountID());
		pk.writeInt(4);
		pk.writeLong(System.currentTimeMillis() / 1000);
		pk.writeDouble(plr.lastPos.x);
		pk.writeDouble(plr.lastPos.y);
		pk.writeDouble(plr.lastPos.z);
		pk.writeString("0");
		pk.writeDouble(plr.lastRot.x);
		pk.writeDouble(plr.lastRot.y);
		pk.writeDouble(plr.lastRot.z);
		pk.writeString("0");
		pk.writeString("0");
		pk.writeString("0");
		pk.writeDouble(plr.lastRot.w);
		switch (action) {
		case "8930": { // Sleep
			plr.lastAction = 40;
			break;
		}
		case "9108": { // Tired
			plr.lastAction = 41;
			break;
		}
		case "9116": { // Sit
			plr.lastAction = 60;
			break;
		}
		case "9121": { // Mad
			plr.lastAction = 70;
			break;
		}
		case "9122": { // Excite
			plr.lastAction = 80;
			break;
		}
		case "9143": { // Sad
			plr.lastAction = 180;
			break;
		}
		case "9151": { // Flex
			plr.lastAction = 200;
			break;
		}
		case "9190": { // Play
			plr.lastAction = 210;
			break;
		}
		case "9147": { // Scared
			plr.lastAction = 190;
			break;
		}
		case "9139": { // Eat
			plr.lastAction = 170;
			break;
		}
		case "9131": { // Yes
			plr.lastAction = 110;
			break;
		}
		case "9135": { // No
			plr.lastAction = 120;
			break;
		}
		}
		pk.writeInt(plr.lastAction);
		pk.writeString(DATA_SUFFIX); // Data suffix

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
