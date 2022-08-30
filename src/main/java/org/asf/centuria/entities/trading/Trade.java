package org.asf.centuria.entities.trading;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.asf.centuria.entities.players.Player;
import org.asf.centuria.packets.xt.gameserver.trading.TradeAddRemoveItemPacket;
import org.asf.centuria.packets.xt.gameserver.trading.TradeExitPacket;
import org.asf.centuria.packets.xt.gameserver.trading.TradeInitiateAcceptPacket;
import org.asf.centuria.packets.xt.gameserver.trading.TradeInitiateCancelPacket;
import org.asf.centuria.packets.xt.gameserver.trading.TradeInitiateFailPacket;
import org.asf.centuria.packets.xt.gameserver.trading.TradeInitiatePacket;
import org.asf.centuria.packets.xt.gameserver.trading.TradeInitiateRejectPacket;

import com.google.gson.JsonElement;

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
	 * If the trade has been started (accepted by both players after initiation)
	 */
	public boolean isStarted;

	/**
	 * Items that the source player is offering.
	 */
	public Map<String, ItemForTrade> itemsToGive = new HashMap<String, ItemForTrade>();

	/**
	 * Items the target player is offering.
	 */
	public Map<String, ItemForTrade> itemsToReceive = new HashMap<String, ItemForTrade>();

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
	 * @throws IOException 
	 */
	public static Trade StartNewTrade(Player sourcePlayer, Player targetPlayer) throws IOException
	{
		if(sourcePlayer.tradeEngagedIn != null || targetPlayer.tradeEngagedIn != null
			|| sourcePlayer.roomReady == false || sourcePlayer.roomReady == false)
		{
			//Players are already engaged in a trade, or arn't fully loaded yet.
			if (System.getProperty("debugMode") != null) {
				System.out.println("[TRADE] [TradeInitiateFailPacket] Server to client.");
			}
			
			TradeInitiateFailPacket tradeInitiateFailPacket = new TradeInitiateFailPacket();

			sourcePlayer.client.sendPacket(tradeInitiateFailPacket);
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
		TradeInitiatePacket tradeInitiatePacket = new TradeInitiatePacket();
		tradeInitiatePacket.success = true;
		tradeInitiatePacket.outboundUserId = sourcePlayer.account.getAccountID();

		targetPlayer.client.sendPacket(tradeInitiatePacket);
		
		if (System.getProperty("debugMode") != null) {
			System.out.println("[TRADE] [TradeInitiate] Server to client with ID " + targetPlayer.account.getAccountID() + ": " + tradeInitiatePacket.build());
		}
		
		//Create trade initiate packet for source player
		tradeInitiatePacket = new TradeInitiatePacket();
		tradeInitiatePacket.success = true;
		
		if (System.getProperty("debugMode") != null) {
			System.out.println("[TRADE] [TradeInitiate] Server to client with ID " + sourcePlayer.account.getAccountID() + ": " + tradeInitiatePacket.build());
		}

		sourcePlayer.client.sendPacket(tradeInitiatePacket);

		return newTrade;
	}

	/**
	 * Cancels a trade (source player reject).
	 * @throws IOException 
	 */
	public void CancelTrade() throws IOException
	{
		TradeInitiateCancelPacket cancelPacket = new TradeInitiateCancelPacket();
				
		//TODO: Does the source player need a cancel packet?
		targetPlayer.client.sendPacket(cancelPacket);
		
		if (System.getProperty("debugMode") != null) {
			System.out.println("[TRADE] [TraceCancel] Server to client with ID " + sourcePlayer.account.getAccountID() + ": " + cancelPacket.build());
		}

		sourcePlayer.tradeEngagedIn = null;
		targetPlayer.tradeEngagedIn = null;
	}
	
	/**
	 * Rejects a trade (target player reject).
	 * @throws IOException 
	 */
	public void RejectTrade() throws IOException
	{
		TradeInitiateRejectPacket rejectPacket = new TradeInitiateRejectPacket();

		//TODO: Does the target player need a reject packet?
		sourcePlayer.client.sendPacket(rejectPacket);

		if (System.getProperty("debugMode") != null) {
			System.out.println("[TRADE] [TradeReject] Server to client with ID " + sourcePlayer.account.getAccountID() + ": " + rejectPacket.build());
		}
		
		sourcePlayer.tradeEngagedIn = null;
		targetPlayer.tradeEngagedIn = null;
	}

	/**
	 * Accepts a trade from a player. Called for both players.
	 * @param acceptedById The player who accepted the trade.
	 */
	public void AcceptTrade(String acceptedById)
	{
		isStarted = true;
		
		TradeInitiateAcceptPacket acceptPacket = new TradeInitiateAcceptPacket();
		
		//tell the source player the trade has been accepted
		sourcePlayer.client.sendPacket(acceptPacket);
	}
	
	/**
	 * Called when the trade is exited by either players.
	 * @param exitedPlayer The player who exited.
	 */
	public void TradeExit(Player exitedPlayer)
	{
		TradeExitPacket tradeExitPacket = new TradeExitPacket();
		
		if(exitedPlayer.account.getAccountID() == sourcePlayer.account.getAccountID())
		{
			targetPlayer.client.sendPacket(tradeExitPacket);
		}
		else
		{
			sourcePlayer.client.sendPacket(tradeExitPacket);
		}

		targetPlayer.tradeEngagedIn = null;
		sourcePlayer.tradeEngagedIn = null;
	}
	
	/**
	 * Add this item to the trade.
	 * @param player The player who is giving them 
	 * @param itemId The item id of the item/
	 * @param item The item's json.
	 * @param quantity The quantity of the item.
	 */
	public void AddItemToTrade(Player player, String itemId, JsonElement item, int quantity)
	{		
		//TODO: IMPLEMENT / FIXES FOR QUANTITY
		ItemForTrade itemForTrade = new ItemForTrade();
		itemForTrade.item = item;
		itemForTrade.quantity = quantity;
		
		TradeAddRemoveItemPacket tradeAddRemovePacket = new TradeAddRemoveItemPacket();
		tradeAddRemovePacket.isAdding = true;
		tradeAddRemovePacket.success = true;
		tradeAddRemovePacket.updatedItem = item;
		tradeAddRemovePacket.inboundQuantity = quantity;
		tradeAddRemovePacket.userId = player.account.getAccountID();
		
		if(player.account.getAccountID() == sourcePlayer.account.getAccountID())
		{
			var itemToGive = itemsToGive.get(itemId);
			
			if(itemToGive != null)
			{
				itemToGive.quantity += quantity;
			}

			targetPlayer.client.sendPacket(tradeAddRemovePacket);
		}
		else
		{
			itemsToReceive.put(itemId, itemForTrade);
			sourcePlayer.client.sendPacket(tradeAddRemovePacket);
		}
	}
	
	/**
	 * Removes an item for trades.
	 * @param player The player who is removing the items.
	 * @param itemId The player item id.
	 * @param quantity The quantity of the item to remove.
	 */
	public void RemoveItemFromTrade(Player player, String itemId, int quantity)
	{	
		TradeAddRemoveItemPacket tradeAddRemovePacket = new TradeAddRemoveItemPacket();
		tradeAddRemovePacket.isAdding = true;
		tradeAddRemovePacket.success = true;
		tradeAddRemovePacket.inboundQuantity = quantity;
		tradeAddRemovePacket.userId = player.account.getAccountID();
		
		if(player.account.getAccountID() == sourcePlayer.account.getAccountID())
		{
			var item = itemsToGive.get(itemId);
			item.quantity -= quantity;
			if(item.quantity <= 0)
			{
				itemsToGive.remove(itemId);
			}
			tradeAddRemovePacket.updatedItem = item.item;
			targetPlayer.client.sendPacket(tradeAddRemovePacket);
		}
		else
		{
			var item = itemsToReceive.get(itemId);
			item.quantity -= quantity;
			if(item.quantity <= 0)
			{
				itemsToReceive.remove(itemId);
			}
			tradeAddRemovePacket.updatedItem = item.item;
			sourcePlayer.client.sendPacket(tradeAddRemovePacket);
		}
	}
}
