package org.asf.emuferal.packets.xt.gameserver.social;

import java.io.IOException;

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
					join.mode = 0;
					join.playerID = player.account.getAccountNumericID();
					join.roomIdentifier = "room_" + join.roomID;

					// Sync
					GameServer srv = (GameServer) client.getServer();
					for (Player plr2 : srv.getPlayers()) {
						if (plr.room != null && player.room != null && plr2.room.equals(plr.room) && plr2 != plr) {
							plr.destroyAt(plr2);
						}
					}

					// Assign room
					player.roomReady = false;
					player.pendingRoomID = plr.roomID;
					player.pendingRoom = "room_" + join.roomID;

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