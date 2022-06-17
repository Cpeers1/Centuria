package org.asf.emuferal.packets.xt.gameserver.sanctuaries;

import java.io.IOException;

import org.asf.emuferal.EmuFeral;
import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.packets.xt.gameserver.world.JoinRoom;
import org.asf.emuferal.players.Player;

public class SanctuaryJoinPacket implements IXtPacket<SanctuaryJoinPacket> {

	public String sanctuaryOwner = null;
	public int mode = 0;

	@Override
	public String id() {
		return "sj";
	}

	@Override
	public SanctuaryJoinPacket instantiate() {
		return new SanctuaryJoinPacket();
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		sanctuaryOwner = reader.read();
		mode = reader.readInt();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Sanctuary join

		// Load player object
		Player player = (Player) client.container;

		// Check owner
		boolean isOwner = player.account.getAccountID().equals(sanctuaryOwner);

		if (!isOwner) {
			// TODO: privacy settings
//			// Send error
//			client.sendPacket("%xt%rj%-1%false%1689%2%-1%" + sanctuaryOwner + "%sanctuary_" + sanctuaryOwner + "%");
//			return true;
		}

		// Find owner
		EmuFeralAccount sancOwner = AccountManager.getInstance().getAccount(sanctuaryOwner);
		if (!sancOwner.getPlayerInventory().containsItem("201")) {
			// Fix sanctuaries
			EmuFeral.fixSanctuaries(sancOwner.getPlayerInventory(), sancOwner);
		}

		// Build room join
		JoinRoom join = new JoinRoom();
		join.levelType = 2;
		join.levelID = 1689;
		join.roomIdentifier = "sanctuary_" + sanctuaryOwner;
		join.teleport = sanctuaryOwner;

		// Sync
		GameServer srv = (GameServer) client.getServer();
		for (Player plr2 : srv.getPlayers()) {
			if (plr2.room != null && player.room != null && player.room != null && plr2.room.equals(player.room)
					&& plr2 != player) {
				player.destroyAt(plr2);
			}
		}

		// Assign room
		player.roomReady = false;
		player.pendingLevelID = 1689;
		player.pendingRoom = "sanctuary_" + sanctuaryOwner;
		player.levelType = join.levelType;

		// Send packet
		client.sendPacket(join);

		return true;
	}

}
