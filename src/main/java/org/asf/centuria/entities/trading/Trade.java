package org.asf.centuria.entities.trading;

import java.util.List;

import org.asf.centuria.entities.inventoryitems.InventoryItem;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.enums.trading.TradeValidationType;
import org.asf.centuria.packets.xt.gameserver.trading.TradeInitateCancelPacket;
import org.asf.centuria.packets.xt.gameserver.trading.TradeInitatePacket;
import org.asf.centuria.packets.xt.gameserver.trading.TradeInitateRejectPacket;

public class Trade {

	/**
	 * The player that is the target of the trade.
	 */
	public Player targetPlayer;

	/**
	 * The player that started the trade.
	 */
	public Player sourcePlayer;

	/**
	 * If the trade has been started (accepted by both players after initation)
	 */
	public boolean isStarted;

	/**
	 * Items that the source player is offering.
	 */
	public List<InventoryItem> itemsToGive;

	/**
	 * Items the target player is offering.
	 */
	public List<InventoryItem> itemsToReceive;

	/**
	 * The ready (initial accept) status for the source player.
	 */
	public boolean readyStatusSource;

	/**
	 * The ready (initial accept) status for the target player.
	 */
	public boolean readyStatusTarget;

	/**
	 * If the trade is in the confirmation (finalizing trade) stage.
	 */
	public boolean isConfirming;

	/**
	 * If the trade has been accepted by the source player. Null if the trade hasn't started.
	 */
	public Boolean isAcceptedBySource;

	/**
	 * If the trade has been accepted by the target player. Null if the trade hasn't started.
	 */
	public Boolean isAcceptedByTarget;

	/**
	 * Chat room ID for the trade's private chat.
	 */
	public String chatConversationId;

	private Trade()
	{ }

	/**
	 * Begins a new trade between two players.
	 * @param sourcePlayer The player who initiated the trade.
	 * @param targetPlayer The player who was targeted for the trade.
	 * @return The new trade.
	 */
	public static Trade StartNewTrade(Player sourcePlayer, Player targetPlayer)
	{
		if(sourcePlayer.tradeEngagedIn != null || targetPlayer.tradeEngagedIn != null
			|| sourcePlayer.roomReady == false || sourcePlayer.roomReady == false)
		{
			//Players are already engaged in a trade, or arn't fully loaded yet.
			TradeInitatePacket tradeInitatePacket = new TradeInitatePacket();
			tradeInitatePacket.success = false;
			tradeInitatePacket.tradeValidationType = TradeValidationType.UserNotAvaliable;
			tradeInitatePacket.userId = targetPlayer.account.getAccountID();

			sourcePlayer.client.sendPacket(tradeInitatePacket);
			return null;
		}

		Trade newTrade = new Trade();
		newTrade.targetPlayer = targetPlayer;
		newTrade.sourcePlayer = sourcePlayer;
		newTrade.isStarted = false;
		newTrade.isConfirming = false;
		newTrade.isAcceptedBySource = false;
		newTrade.isAcceptedByTarget = false;

		targetPlayer.tradeEngagedIn = newTrade;
		sourcePlayer.tradeEngagedIn = newTrade;

		//Create new trade initiate packet for target player
		TradeInitatePacket tradeInitatePacket = new TradeInitatePacket();
		tradeInitatePacket.success = true;
		tradeInitatePacket.tradeValidationType = TradeValidationType.Success;
		tradeInitatePacket.userId = sourcePlayer.account.getAccountID();

		targetPlayer.client.sendPacket(tradeInitatePacket);

		//Create trade intiate packet for source player
		tradeInitatePacket = new TradeInitatePacket();
		tradeInitatePacket.success = true;
		tradeInitatePacket.tradeValidationType = TradeValidationType.Success;
		tradeInitatePacket.userId = targetPlayer.account.getAccountID();

		targetPlayer.client.sendPacket(tradeInitatePacket);

		return newTrade;
	}

	/**
	 * Accepts a trade from a player. Called for both players.
	 * @param acceptedById The player who accepted the trade.
	 */
	public void AcceptTrade(String acceptedById)
	{
		if(acceptedById.equals(sourcePlayer.account.getAccountID()))
		{
			readyStatusSource = true;
		}
		else if(acceptedById.equals(targetPlayer.account.getAccountID()))
		{
			readyStatusTarget = true;
		}

		if(readyStatusSource && readyStatusTarget)
		{
			//TODO: Trade Accept Logic
			isConfirming = false;
			isStarted = true;
			isAcceptedBySource = false;
			isAcceptedByTarget = false;
		}
	}

	/**
	 * Cancels a trade (source player reject).
	 */
	public void CancelTrade()
	{
		TradeInitateCancelPacket cancelPacket = new TradeInitateCancelPacket();
		
		//TODO: Does the source player need a cancel packet?
		targetPlayer.client.sendPacket(cancelPacket);

		sourcePlayer.tradeEngagedIn = null;
		targetPlayer.tradeEngagedIn = null;
	}
	
	/**
	 * Rejects a trade (target player reject).
	 */
	public void RejectTrade()
	{
		TradeInitateRejectPacket rejectPacket = new TradeInitateRejectPacket();

		//TODO: Does the target player need a reject packet?
		sourcePlayer.client.sendPacket(rejectPacket);

		sourcePlayer.tradeEngagedIn = null;
		targetPlayer.tradeEngagedIn = null;
	}
}
