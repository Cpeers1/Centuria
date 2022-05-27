package org.asf.emuferal.packets.xt.gameserver.players;

import java.io.IOException;
import java.util.UUID;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.packets.xt.gameserver.world.JoinRoom;
import org.asf.emuferal.players.Player;

public class JumpToPlayer implements IXtPacket<JumpToPlayer> {

	private String accountID;

	@Override
	public JumpToPlayer instantiate() {
		return new JumpToPlayer();
	}

	@Override
	public String id() {
		return "rfjtr";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		accountID = reader.readRemaining();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		Player player = ((Player) client.container);

		// Find player
		for (Player plr : ((GameServer) client.getServer()).getPlayers()) {
			if (plr.account.getAccountID().equals(accountID) && plr.roomReady) { // TODO: privacy settings
				XtWriter writer = new XtWriter();
				writer.writeString("rfjtr");
				writer.writeInt(-1); // data prefix
				// Check world
				if (plr.room.equals(player.room)) {
					writer.writeInt(1); // current world
					writer.writeString("");
				} else {
					writer.writeInt(1); // other world
					writer.writeString(plr.account.getAccountID());
				}
				writer.writeString(""); // data suffix
				client.sendPacket(writer.encode());

				if (!plr.room.equals(player.room)) {
					// Build room join
					JoinRoom join = new JoinRoom();
					join.playerID = player.account.getAccountNumericID();
					join.roomID = plr.roomID;
					join.mode = 0;
					join.roomIdentifier = "room_" + UUID.nameUUIDFromBytes(player.room.getBytes("UTF-8"));

					// Switch player info over
					player.roomID = plr.roomID;
					player.room = plr.room;
					player.roomReady = false;

					// SEnd packet
					client.sendPacket(join);
				}
				return true;
			}
		}

		XtWriter writer = new XtWriter();
		writer.writeString("rfjtr");
		writer.writeInt(-1); // data prefix
		writer.writeInt(0); // failure
		writer.writeString(""); // data suffix
		client.sendPacket(writer.encode());
		return true; // Account not found or loading
	}

}