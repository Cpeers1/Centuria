package org.asf.centuria.packets.xt.gameserver.sanctuary;

import java.io.IOException;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.entities.uservars.UserVarValue;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;
import org.asf.centuria.packets.xt.gameserver.room.RoomJoinPacket;
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
		writer.writeInt(DATA_PREFIX); // Data prefix

		writer.writeBoolean(success);

		writer.writeString(DATA_SUFFIX); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {

		try {

			// need to use the twiggle to find out what was worked on

			var player = (Player) client.container;

			var twiggleItem = player.account.getSaveSpecificInventory().getTwiggleAccesor().getTwiggle(twiggleInvId);

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
				var didSucceed = player.account.getSaveSpecificInventory().getSanctuaryAccessor()
						.upgradeSanctuaryToStage(
								twiggleItem.getTwiggleComponent().twiggleWorkParams.classItemInvId,
								twiggleItem.getTwiggleComponent().twiggleWorkParams.stage);

				if (didSucceed) {
					sendIlPacket(player);
				} else {
					// failed to complete upgrade
					this.success = false;
					client.sendPacket(this);
					return true;
				}
			} else if (twiggleItem.getTwiggleComponent().twiggleWorkParams.enlargedAreaIndex != null) {
				// its a room expansion
				var didSucceed = player.account.getSaveSpecificInventory().getSanctuaryAccessor()
						.expandSanctuaryRoom(
								twiggleItem.getTwiggleComponent().twiggleWorkParams.classItemInvId,
								twiggleItem.getTwiggleComponent().twiggleWorkParams.enlargedAreaIndex);

				if (didSucceed) {
					sendIlPacket(player);
				} else {
					// failed to complete expansion
					this.success = false;
					client.sendPacket(this);
					return true;
				}
			}

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

	public void sendIlPacket(Player player) {
		var twiggleAccessor = player.account.getSaveSpecificInventory().getTwiggleAccesor();

		var il = player.account.getSaveSpecificInventory().getItem("201");
		var ilPacket = new InventoryItemPacket();
		ilPacket.item = il;

		// send IL
		player.client.sendPacket(ilPacket);

		il = player.account.getSaveSpecificInventory().getItem("5");
		ilPacket = new InventoryItemPacket();
		ilPacket.item = il;

		// send IL
		player.client.sendPacket(ilPacket);

		il = player.account.getSaveSpecificInventory().getItem("10");
		ilPacket = new InventoryItemPacket();
		ilPacket.item = il;

		// send IL
		player.client.sendPacket(ilPacket);

		if (twiggleAccessor.getTwiggle(twiggleInvId) != null) {
			twiggleAccessor.clearTwiggleWork(twiggleInvId);
		}
		il = player.account.getSaveSpecificInventory().getItem("110");
		ilPacket = new InventoryItemPacket();
		ilPacket.item = il;

		// send IL
		player.client.sendPacket(ilPacket);
	}

	public void JoinSanctuary(SmartfoxClient client, String sanctuaryOwner) {
		var isAllowed = true;

		// Load player object
		Player player = (Player) client.container;

		// Find owner
		CenturiaAccount sancOwner = AccountManager.getInstance().getAccount(sanctuaryOwner);
		if (!sancOwner.getSaveSpecificInventory().containsItem("201")) {
			Player plr = sancOwner.getOnlinePlayerInstance();
			if (plr != null)
				plr.activeSanctuaryLook = sancOwner.getActiveSanctuaryLook();
		}

		// Check owner
		boolean isOwner = player.account.getAccountID().equals(sanctuaryOwner);

		if (!isOwner) {
			// Load privacy settings
			int privSetting = 0;
			UserVarValue val = sancOwner.getSaveSpecificInventory().getUserVarAccesor().getPlayerVarValue(17544, 0);
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
		RoomJoinPacket join = new RoomJoinPacket();
		join.success = isAllowed;
		join.levelType = 2;
		join.levelID = 1689;
		join.roomIdentifier = "sanctuary_" + sanctuaryOwner;
		join.teleport = sanctuaryOwner;

		if (isAllowed) {
			// Sync
			GameServer srv = (GameServer) client.getServer();
			for (Player plr2 : srv.getPlayers()) {
				if (plr2.room != null && player.room != null && player.room != null && plr2.room.equals(player.room)) {
					plr2.teleportToSanctuary(sanctuaryOwner);
				}
			}
		} else
			// Send packet
			client.sendPacket(join);
	}

}
