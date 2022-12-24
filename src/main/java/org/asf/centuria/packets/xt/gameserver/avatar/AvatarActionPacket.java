package org.asf.centuria.packets.xt.gameserver.avatar;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.packets.xt.gameserver.object.ObjectUpdatePacket;

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

		int actionId = 0;
		switch (action) {
		case "8930": { // Sleep
			actionId = 40;
			break;
		}
		case "9108": { // Tired
			actionId = 41;
			break;
		}
		case "9116": { // Sit
			actionId = 60;
			break;
		}
		case "9121": { // Mad
			actionId = 70;
			break;
		}
		case "9122": { // Excite
			actionId = 80;
			break;
		}
		case "9143": { // Sad
			actionId = 180;
			break;
		}
		case "9151": { // Flex
			actionId = 200;
			break;
		}
		case "9190": { // Play
			actionId = 210;
			break;
		}
		case "9147": { // Scared
			actionId = 190;
			break;
		}
		case "9139": { // Eat
			actionId = 170;
			break;
		}
		case "9131": { // Yes
			actionId = 110;
			break;
		}
		case "9135": { // No
			actionId = 120;
			break;
		}
		}
		plr.lastAction = actionId;

		// Create packet
		ObjectUpdatePacket pkt = new ObjectUpdatePacket();
		pkt.action = actionId;
		pkt.id = plr.account.getAccountID();
		pkt.mode = 4;
		pkt.position = plr.lastPos;
		pkt.rotation = plr.lastRot;
		pkt.time = System.currentTimeMillis();
		pkt.speed = 20;

		// Broadcast sync
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				player.client.sendPacket(pkt);
			}
		}

		return true;
	}

}
