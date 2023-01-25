package org.asf.centuria.packets.xt.gameserver.trade;

import java.io.IOException;

import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.entities.trading.Trade;
import org.asf.centuria.enums.trading.TradeValidationType;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.social.SocialManager;

public class TradeInitiatePacket implements IXtPacket<TradeInitiatePacket> {

	private static final String PACKET_ID = "ti";

	// Inbound
	public String inboundUserId;

	// Outbound
	public TradeValidationType tradeValidationType = TradeValidationType.Success;
	public String outboundUserId = null;

	@Override
	public TradeInitiatePacket instantiate() {
		return new TradeInitiatePacket();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		inboundUserId = reader.read();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(DATA_PREFIX); // Data prefix
		writer.writeString(outboundUserId); // user ID
		writer.writeInt(tradeValidationType.value); // trade validation type
		writer.writeBoolean(tradeValidationType == TradeValidationType.Success);
		writer.writeString(DATA_SUFFIX); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {

		if (System.getProperty("debugMode") != null) {
			System.out.println("[TRADE] [TradeInitiate] Client to server: ( inboundUserId: " + inboundUserId + " )");
		}

		Player sourcePlayer = ((Player) client.container);

		// Inbound user ID is the target player to trade
		Player targetPlayer = AccountManager.getInstance().getAccount(inboundUserId).getOnlinePlayerInstance();
		if (targetPlayer == null
				|| !sourcePlayer.account.getSaveSpecificInventory().getSaveSettings().tradeLockID
						.equals(targetPlayer.account.getSaveSpecificInventory().getSaveSettings().tradeLockID)
				|| SocialManager.getInstance()
						.getPlayerIsBlocked(targetPlayer.account.getAccountID(),
						sourcePlayer.account.getAccountID())) {
			// Fail
			TradeInitiateFailPacket pk = new TradeInitiateFailPacket();
			pk.player = inboundUserId;
			pk.tradeValidationType = TradeValidationType.User_Not_Avail;
			sourcePlayer.client.sendPacket(pk);
			return true;
		}

		// Check privacy settings
		int privSetting = 0;
		if (targetPlayer.account.getSaveSpecificInventory().getUserVarAccesor().getPlayerVarValue(17545, 0) != null)
			privSetting = targetPlayer.account.getSaveSpecificInventory().getUserVarAccesor().getPlayerVarValue(17545,
					0).value;
		if ((privSetting == 1 && !SocialManager.getInstance().getPlayerIsFollowing(targetPlayer.account.getAccountID(),
				sourcePlayer.account.getAccountID())) || privSetting == 2) {
			// Fail
			TradeInitiateFailPacket pk = new TradeInitiateFailPacket();
			pk.player = inboundUserId;
			pk.tradeValidationType = TradeValidationType.User_Not_Avail;
			sourcePlayer.client.sendPacket(pk);
			return true;
		}

		// Start a new trade.
		Trade.startNewTrade(sourcePlayer, targetPlayer);

		return true;
	}

}
