package org.asf.emuferal.packets.xt.gameserver.social;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.entities.uservars.UserVarValue;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.packets.xt.gameserver.world.JoinRoom;
import org.asf.emuferal.players.Player;
import org.asf.emuferal.social.SocialManager;

public class JumpToPlayer implements IXtPacket<JumpToPlayer> {

	private static final String PACKET_ID = "rfjtr";

	private String accountID;

	@Override
	public JumpToPlayer instantiate() {
		return new JumpToPlayer();
	}

	@Override
	public String id() {
		return PACKET_ID;
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
							.getPlayerIsBlocked(accountID, player.account.getAccountID()))) {
				// Load privacy settings
				int privSetting = 0;
				UserVarValue val = plr.account.getPlayerInventory().getUserVarAccesor().getPlayerVarValue(17546, 0);
				if (val != null)
					privSetting = val.value;

				// Verify privacy settings
				if (privSetting == 1 && !SocialManager.getInstance().getPlayerIsFollowing(plr.account.getAccountID(),
						player.account.getAccountID()))
					break;
				else if (privSetting == 2)
					break;

				XtWriter writer = new XtWriter();
				writer.writeString("rfjtr");
				writer.writeInt(-1); // data prefix
				writer.writeInt(1); // other world
				writer.writeString("");
				writer.writeString(""); // data suffix
				client.sendPacket(writer.encode());

				if (!plr.room.equals(player.room)) {
					// Build room join
					JoinRoom join = new JoinRoom();
					join.levelType = plr.levelType;
					join.levelID = plr.levelID;
					join.roomIdentifier = "room_" + join.levelID;
					player.teleportDestination = plr.account.getAccountID();

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
		return true; // Account not found, blocked or still loading
	}

}
