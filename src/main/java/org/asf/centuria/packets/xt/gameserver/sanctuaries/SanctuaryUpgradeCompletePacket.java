package org.asf.centuria.packets.xt.gameserver.sanctuaries;

import java.io.IOException;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.uservars.UserVarValue;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.centuria.packets.xt.gameserver.world.JoinRoom;
import org.asf.centuria.players.Player;
import org.asf.centuria.social.SocialManager;

public class SanctuaryUpgradeCompletePacket implements IXtPacket<SanctuaryUpgradeCompletePacket> {

	private static final String PACKET_ID = "suc";

	public String twiggleInvId;

	public boolean success;

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public SanctuaryUpgradeCompletePacket instantiate() {
		return new SanctuaryUpgradeCompletePacket();
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		twiggleInvId = reader.read();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1); // Data prefix

		writer.writeBoolean(success);

		writer.writeString(""); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {

		try {

			// need to use the twiggle to find out what was worked on

			var player = (Player) client.container;
			var twiggleAccessor = player.account.getPlayerInventory().getTwiggleAccesor();

			var twiggleItem = twiggleAccessor.getTwiggle(twiggleInvId);

			switch (twiggleItem.getTwiggleComponent().workType) {
			case WorkingOtherSanctuary:
			case WorkingSanctuary:
				// we are ok
				break;
			case FinishedOtherSanctuary:
			case FinishedSanctuary:
			case None:
			default: {
				// failed to complete expansion
				this.success = false;
				client.sendPacket(this);
				return true;
			}
			}

			if (twiggleItem.getTwiggleComponent().twiggleWorkParams.stage != null) {
				// its a stage upgrade
				var didSucceed = player.account.getPlayerInventory().getSanctuaryAccessor().upgradeSanctuaryToStage(
						twiggleItem.getTwiggleComponent().twiggleWorkParams.classItemInvId,
						twiggleItem.getTwiggleComponent().twiggleWorkParams.stage);

				if (didSucceed) {
					var il = player.account.getPlayerInventory().getItem("201");
					var ilPacket = new InventoryItemPacket();
					ilPacket.item = il;

					// send IL
					player.client.sendPacket(ilPacket);

					il = player.account.getPlayerInventory().getItem("5");
					ilPacket = new InventoryItemPacket();
					ilPacket.item = il;

					// send IL
					player.client.sendPacket(ilPacket);

					il = player.account.getPlayerInventory().getItem("10");
					ilPacket = new InventoryItemPacket();
					ilPacket.item = il;

					// send IL
					player.client.sendPacket(ilPacket);

					il = player.account.getPlayerInventory().getItem("110");
					ilPacket = new InventoryItemPacket();
					ilPacket.item = il;

					// send IL
					player.client.sendPacket(ilPacket);
				} else {
					// failed to complete expansion
					this.success = false;
					client.sendPacket(this);
					return true;
				}
			} else if (twiggleItem.getTwiggleComponent().twiggleWorkParams.enlargedAreaIndex != null) {
				// its a stage upgrade
				var didSucceed = player.account.getPlayerInventory().getSanctuaryAccessor().enlargenSanctuaryRooms(
						twiggleItem.getTwiggleComponent().twiggleWorkParams.classItemInvId,
						twiggleItem.getTwiggleComponent().twiggleWorkParams.enlargedAreaIndex);

				if (didSucceed) {
					var il = player.account.getPlayerInventory().getItem("201");
					var ilPacket = new InventoryItemPacket();
					ilPacket.item = il;

					// send IL
					player.client.sendPacket(ilPacket);

					il = player.account.getPlayerInventory().getItem("5");
					ilPacket = new InventoryItemPacket();
					ilPacket.item = il;

					// send IL
					player.client.sendPacket(ilPacket);

					il = player.account.getPlayerInventory().getItem("10");
					ilPacket = new InventoryItemPacket();
					ilPacket.item = il;

					// send IL
					player.client.sendPacket(ilPacket);

					il = player.account.getPlayerInventory().getItem("110");
					ilPacket = new InventoryItemPacket();
					ilPacket.item = il;

					// send IL
					player.client.sendPacket(ilPacket);
				} else {
					// failed to complete expansion
					this.success = false;
					client.sendPacket(this);
					return true;
				}
			}

			twiggleAccessor.clearTwiggleWork(twiggleInvId);
			this.success = true;
			player.client.sendPacket(this);

			JoinSanctuary(client, player.account.getAccountID());

		} catch (Exception e) {
			e.printStackTrace();

			// failed to complete expansion
			this.success = false;
			client.sendPacket(this);
		}

		return true;
	}

	public void JoinSanctuary(SmartfoxClient client, String sanctuaryOwner) {
		var isAllowed = true;

		// Load player object
		Player player = (Player) client.container;

		// Find owner
		CenturiaAccount sancOwner = AccountManager.getInstance().getAccount(sanctuaryOwner);
		if (!sancOwner.getPlayerInventory().containsItem("201")) {
			Player plr = sancOwner.getOnlinePlayerInstance();
			if (plr != null)
				plr.activeSanctuaryLook = sancOwner.getActiveSanctuaryLook();
		}

		// Check owner
		boolean isOwner = player.account.getAccountID().equals(sanctuaryOwner);

		if (!isOwner) {
			// Load privacy settings
			int privSetting = 0;
			UserVarValue val = sancOwner.getPlayerInventory().getUserVarAccesor().getPlayerVarValue(17544, 0);
			if (val != null)
				privSetting = val.value;

			// Verify access
			if (privSetting == 2) {
				// Nobody
				isAllowed = false;
			} else if (privSetting == 1) {
				// Followers
				// Check if the owner follows the current player
				if (!SocialManager.getInstance().getPlayerIsFollowing(sanctuaryOwner, player.account.getAccountID())) {
					isAllowed = false;
				}
			} else
				// Everyone
				isAllowed = true;
		} else
			isAllowed = true;

		// Build room join
		JoinRoom join = new JoinRoom();
		join.success = isAllowed;
		join.levelType = 2;
		join.levelID = 1689;
		join.roomIdentifier = "sanctuary_" + sanctuaryOwner;
		join.teleport = sanctuaryOwner;

		if (isAllowed == true) {
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
		}
		// Send packet
		client.sendPacket(join);
	}

}
