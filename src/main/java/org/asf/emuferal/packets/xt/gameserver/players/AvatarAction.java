package org.asf.emuferal.packets.xt.gameserver.players;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class AvatarAction implements IXtPacket<AvatarAction> {

	private String action;

	@Override
	public AvatarAction instantiate() {
		return new AvatarAction();
	}

	@Override
	public String id() {
		return "aa";
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
