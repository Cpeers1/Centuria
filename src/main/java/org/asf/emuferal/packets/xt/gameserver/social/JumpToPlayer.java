package org.asf.emuferal.packets.xt.gameserver.social;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.packets.xt.gameserver.world.JoinRoom;
import org.asf.emuferal.players.Player;
import org.asf.emuferal.social.SocialManager;

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
			if (plr.account.getAccountID().equals(accountID) && plr.roomReady && !plr.room.equals("room_STAFFROOM")
					&& (!SocialManager.getInstance().socialListExists(accountID) || !SocialManager.getInstance()
							.getPlayerIsBlocked(accountID, player.account.getAccountID()))) { // TODO: privacy settings
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
					join.roomType = plr.levelType;
					join.roomID = plr.levelID;
					join.roomIdentifier = "room_" + join.roomID;
					join.teleport = plr.account.getAccountID(); // TODO: get this to work

					// Sync
					GameServer srv = (GameServer) client.getServer();
					for (Player plr2 : srv.getPlayers()) {
						if (plr2.room != null && player.room != null && player.room != null
								&& plr2.room.equals(player.room) && plr2 != player) {
							player.destroyAt(plr2);
						}
					}

					// Assign room
					player.roomReady = false;
					player.pendingLevelID = plr.levelID;
					player.pendingRoom = plr.room;
					player.levelType = plr.levelType;

					// Send packet
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
