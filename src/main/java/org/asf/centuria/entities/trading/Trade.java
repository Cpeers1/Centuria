package org.asf.centuria.entities.trading;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.MarkerManager;
import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.enums.trading.TradeValidationType;
import org.asf.centuria.modules.eventbus.EventBus;
import org.asf.centuria.modules.events.accounts.AccountTradeBanEvent;
import org.asf.centuria.modules.events.accounts.AccountTradePardonEvent;
import org.asf.centuria.packets.xt.gameserver.trade.*;

import com.google.gson.JsonObject;

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
	 * If the trade has been accepted by the source player. Null if the trade hasn't
	 * started.
	 */
	public Boolean isAcceptedBySource;

	/**
	 * If the trade has been accepted by the target player. Null if the trade hasn't
	 * started.
	 */
	public Boolean isAcceptedByTarget;

	/**
	 * Chat room ID for the trade's private chat.
	 */
	public String chatConversationId;

	private Trade() {
	}

	/**
	 * Temporarily bans a player from trading
	 *
	 * @param player Player account instance
	 * @param days   How long to ban the player from trading in days
	 */
	public static void tradeBanTemp(CenturiaAccount player, int days) {
		tradeBanTemp(player, days, null);
	}

	/**
	 * Temporarily bans a player from trading
	 *
	 * @param player Player account instance
	 * @param days   How long to ban the player from trading in days
	 * @param reason Ban reason
	 */
	public static void tradeBanTemp(CenturiaAccount player, int days, String reason) {
		tradeBanTemp(player, days, "SYSTEM", reason);
	}

	/**
	 * Temporarily bans a player from trading
	 *
	 * @param player Player account instance
	 * @param days   How long to ban the player from trading in days
	 * @param issuer Ban issuer
	 * @param reason Ban reason
	 */
	public static void tradeBanTemp(CenturiaAccount player, int days, String issuer, String reason) {
		// Ban the player
		JsonObject banInfo = new JsonObject();
		if (reason != null)
			banInfo.addProperty("reason", reason);
		banInfo.addProperty("unbanTimestamp", System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000));
		player.getSaveSharedInventory().setItem("tradepenalty", banInfo);

		// Find online player
		Player plr = player.getOnlinePlayerInstance();
		if (plr != null) {
			// Apply to ingame player
			if (plr.account != player)
				plr.account.getSaveSharedInventory().setItem("tradepenalty", banInfo);
		}

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new AccountTradeBanEvent(player, days, issuer, reason));

		// Log
		String issuerNm = issuer;
		if (!issuerNm.equals("SYSTEM")) {
			CenturiaAccount acc = AccountManager.getInstance().getAccount(issuer);
			if (acc != null)
				issuerNm = acc.getDisplayName();
		}
		Centuria.logger.info("Temporarily trade-banned " + player.getDisplayName() + ": "
				+ (reason == null ? "Unspecified reason" : reason) + " (issued by " + issuerNm + ", unban in " + days
				+ " days)");
	}

	/**
	 * Permanently bans a player from trading
	 *
	 * @param player Player account instance
	 */
	public static void tradeBanPermanent(CenturiaAccount player) {
		tradeBanPermanent(player, null);
	}

	/**
	 * Permanently bans a player from trading
	 *
	 * @param player Player account instance
	 * @param reason Ban reason
	 */
	public static void tradeBanPermanent(CenturiaAccount player, String reason) {
		tradeBanPermanent(player, "SYSTEM", reason);
	}

	/**
	 * Permanently bans a player from trading
	 *
	 * @param player Player account instance
	 * @param issuer Ban issuer
	 * @param reason Ban reason
	 */
	public static void tradeBanPermanent(CenturiaAccount player, String issuer, String reason) {
		// Ban the player
		JsonObject banInfo = new JsonObject();
		if (reason != null)
			banInfo.addProperty("reason", reason);
		banInfo.addProperty("unbanTimestamp", -1);
		player.getSaveSharedInventory().setItem("tradepenalty", banInfo);

		// Find online player
		Player plr = player.getOnlinePlayerInstance();
		if (plr != null) {
			// Apply to ingame player
			if (plr.account != player)
				plr.account.getSaveSharedInventory().setItem("tradepenalty", banInfo);
		}

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new AccountTradeBanEvent(player, -1, issuer, reason));

		// Log
		String issuerNm = issuer;
		if (!issuerNm.equals("SYSTEM")) {
			CenturiaAccount acc = AccountManager.getInstance().getAccount(issuer);
			if (acc != null)
				issuerNm = acc.getDisplayName();
		}
		Centuria.logger.info("Permanently trade-banned " + player.getDisplayName() + ": "
				+ (reason == null ? "Unspecified reason" : reason) + " (issued by " + issuerNm + ")");
	}

	/**
	 * Removes a trade ban from a player
	 *
	 * @param player Player account instance
	 */
	public static void tradeBanPardon(CenturiaAccount player) {
		tradeBanPardon(player, null);
	}

	/**
	 * Removes a trade ban from a player
	 *
	 * @param player Player account instance
	 * @param reason Pardon reason
	 */
	public static void tradeBanPardon(CenturiaAccount player, String reason) {
		tradeBanPardon(player, "SYSTEM", reason);
	}

	/**
	 * Removes a trade ban from a player
	 *
	 * @param player Player account instance
	 * @param issuer Pardon issuer
	 * @param reason Pardon reason
	 */
	public static void tradeBanPardon(CenturiaAccount player, String issuer, String reason) {
		// Check penalty
		boolean wasPardoned = false;
		if (!isTradeBanned(player))
			wasPardoned = true;

		// Remove penalties
		if (player.getSaveSharedInventory().containsItem("tradepenalty"))
			player.getSaveSharedInventory().deleteItem("tradepenalty");

		// Find online player
		Player plr = player.getOnlinePlayerInstance();
		if (plr != null) {
			// Apply to ingame player
			if (plr.account != player) {
				// Remove penalties
				if (plr.account.getSaveSharedInventory().containsItem("tradepenalty"))
					plr.account.getSaveSharedInventory().deleteItem("tradepenalty");
			}
		}

		// Check state
		if (wasPardoned)
			return;

		// Dispatch event
		EventBus.getInstance().dispatchEvent(new AccountTradePardonEvent(player, issuer, reason));

		// Log
		String issuerNm = issuer;
		if (!issuerNm.equals("SYSTEM")) {
			CenturiaAccount acc = AccountManager.getInstance().getAccount(issuer);
			if (acc != null)
				issuerNm = acc.getDisplayName();
		}
		Centuria.logger.info("Removed the trade ban from " + player.getDisplayName() + ": "
				+ (reason == null ? "Unspecified reason" : reason) + " (issued by " + issuerNm + ")");
	}

	/**
	 * Checks if the trade ban of a player is permanent
	 * 
	 * @param player Player account instance
	 * @return True if trading has been permanently banned for this player, false
	 *         otherwise
	 */
	public static boolean isTradeBanPermanent(CenturiaAccount player) {
		// Check trade ban
		if (player.getSaveSharedInventory().containsItem("tradepenalty")) {
			PlayerInventory inv = player.getSaveSharedInventory();
			JsonObject penalty = inv.getItem("tradepenalty").getAsJsonObject();
			if (penalty.has("unbanTimestamp") && penalty.get("unbanTimestamp").getAsLong() != -1) {
				// Temporary
				return false;
			}

			// Permanent
			return true;
		}

		// Not trade banned
		return false;
	}

	/**
	 * Checks if the given player is trade-banned
	 * 
	 * @param player Player account instance
	 * @return True if trading has been blocked for this player, false otherwise
	 */
	public static boolean isTradeBanned(CenturiaAccount player) {
		// Check trade ban
		if (player.getSaveSharedInventory().containsItem("tradepenalty")) {
			PlayerInventory inv = player.getSaveSharedInventory();
			JsonObject penalty = inv.getItem("tradepenalty").getAsJsonObject();
			if (penalty.has("unbanTimestamp") && penalty.get("unbanTimestamp").getAsLong() != -1
					&& System.currentTimeMillis() > penalty.get("unbanTimestamp").getAsLong()) {
				// Remove
				inv.deleteItem("tradepenalty");

				// Expired
				return false;
			}

			// Ban still valid
			return true;
		}

		// Not trade banned
		return false;
	}

	/**
	 * Begins a new trade between two players.
	 * 
	 * @param sourcePlayer The player who initiated the trade.
	 * @param targetPlayer The player who was targeted for the trade.
	 * @return The new trade.
	 * @throws IOException
	 */
	public static Trade startNewTrade(Player sourcePlayer, Player targetPlayer) throws IOException {
		if (targetPlayer == null || sourcePlayer.tradeEngagedIn != null || targetPlayer.tradeEngagedIn != null
				|| sourcePlayer.roomReady == false || sourcePlayer.roomReady == false) {
			// Players are already engaged in a trade, or arn't fully loaded yet.
			Centuria.logger.debug(MarkerManager.getMarker("TRADE"), "[TradeInitiateFailPacket] Server to client.");
			TradeInitiateFailPacket pk = new TradeInitiateFailPacket();
			pk.player = targetPlayer.account.getAccountID();
			pk.tradeValidationType = TradeValidationType.User_Not_Avail;
			sourcePlayer.client.sendPacket(pk);
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

		// Create new trade initiate packet for target player
		TradeInitiatePacket tradeInitiatePacket = new TradeInitiatePacket();
		tradeInitiatePacket.outboundUserId = sourcePlayer.account.getAccountID();

		targetPlayer.client.sendPacket(tradeInitiatePacket);
		Centuria.logger.debug(MarkerManager.getMarker("TRADE"), "[TradeInitiate] Server to client with ID "
				+ targetPlayer.account.getAccountID() + ": " + tradeInitiatePacket.build());

		// Create trade initiate packet for source player
		tradeInitiatePacket = new TradeInitiatePacket();
		tradeInitiatePacket.tradeValidationType = TradeValidationType.Success;
		Centuria.logger.debug(MarkerManager.getMarker("TRADE"), "[TradeInitiate] Server to client with ID "
				+ sourcePlayer.account.getAccountID() + ": " + tradeInitiatePacket.build());
		sourcePlayer.client.sendPacket(tradeInitiatePacket);

		return newTrade;
	}

	/**
	 * Cancels a trade (source player reject).
	 * 
	 * @throws IOException
	 */
	public void cancelTrade() throws IOException {
		TradeInitiateCancelPacket cancelPacket = new TradeInitiateCancelPacket();

		// TODO: Does the source player need a cancel packet?
		targetPlayer.client.sendPacket(cancelPacket);
		Centuria.logger.debug(MarkerManager.getMarker("TRADE"), "[TraceCancel] Server to client with ID "
				+ sourcePlayer.account.getAccountID() + ": " + cancelPacket.build());

		sourcePlayer.tradeEngagedIn = null;
		targetPlayer.tradeEngagedIn = null;
	}

	/**
	 * Rejects a trade (target player reject).
	 * 
	 * @throws IOException
	 */
	public void rejectTrade() throws IOException {
		TradeInitiateRejectPacket rejectPacket = new TradeInitiateRejectPacket();

		// TODO: Does the target player need a reject packet?
		sourcePlayer.client.sendPacket(rejectPacket);
		Centuria.logger.debug(MarkerManager.getMarker("TRADE"),
				"Server to client with ID " + sourcePlayer.account.getAccountID() + ": " + rejectPacket.build());

		sourcePlayer.tradeEngagedIn = null;
		targetPlayer.tradeEngagedIn = null;
	}

	/**
	 * Accepts a trade from a player. Called for both players.
	 * 
	 * @param acceptedById The player who accepted the trade.
	 * @throws IOException
	 */
	public void tradeAccept(String acceptedById) throws IOException {
		isStarted = true;

		TradeInitiateAcceptPacket acceptPacket = new TradeInitiateAcceptPacket();

		// tell the source player the trade has been accepted
		sourcePlayer.client.sendPacket(acceptPacket);

		Centuria.logger.debug(MarkerManager.getMarker("TRADE"), "[TradeAccept] Server to client of player "
				+ sourcePlayer.account.getDisplayName() + ": " + acceptPacket.build());
	}

	/**
	 * Called when the trade is exited by either players.
	 * 
	 * @param exitedPlayer The player who exited.
	 * @throws IOException
	 */
	public void tradeExit(Player exitedPlayer) throws IOException {
		TradeExitPacket tradeExitPacket = new TradeExitPacket();

		if (exitedPlayer.account.getAccountID().equals(sourcePlayer.account.getAccountID())) {
			targetPlayer.client.sendPacket(tradeExitPacket);

			Centuria.logger.debug(MarkerManager.getMarker("TRADE"), "[TradeExit] Server to client of player "
					+ targetPlayer.account.getDisplayName() + ": " + tradeExitPacket.build());
		} else {
			sourcePlayer.client.sendPacket(tradeExitPacket);

			Centuria.logger.debug(MarkerManager.getMarker("TRADE"), "[TradeExit] Server to client of player "
					+ sourcePlayer.account.getDisplayName() + ": " + tradeExitPacket.build());
		}

		targetPlayer.tradeEngagedIn = null;
		sourcePlayer.tradeEngagedIn = null;
	}

	/**
	 * Add this item to the trade.
	 * 
	 * @param player   The player who is giving them
	 * @param itemId   The item id of the item.
	 * @param item     The item's json.
	 * @param quantity The quantity of the item.
	 * @throws IOException
	 */
	public void addItemToTrade(Player player, String itemId, JsonObject item, int quantity) throws IOException {
		// Check validity
		if (quantity < 0)
			return;

		// Verify quantity
		int ownedQuant = 0;
		if (item.has("defId")) {
			// Get quantity
			ownedQuant = player.account.getSaveSpecificInventory().getItemAccessor(player)
					.getCountOfItem(item.get("defId").getAsInt());
		}

		// Reset to max if needed
		if (quantity > ownedQuant)
			quantity = ownedQuant;

		// Add
		TradeAddRemoveItemPacket tradeAddRemovePacket = new TradeAddRemoveItemPacket();
		tradeAddRemovePacket.isAdding = 1;
		tradeAddRemovePacket.success = true;
		tradeAddRemovePacket.updatedItem = item;
		tradeAddRemovePacket.quantity = quantity;
		tradeAddRemovePacket.userId = player.account.getAccountID();

		// Send to sender
		player.client.sendPacket(tradeAddRemovePacket);

		// Send to receiver
		if (player.account.getAccountID().equals(sourcePlayer.account.getAccountID())) {
			var itemToGive = itemsToGive.get(itemId);

			if (itemToGive != null) {
				itemToGive.quantity += quantity;
			} else {
				ItemForTrade itemForTrade = new ItemForTrade();
				itemForTrade.item = item;
				itemForTrade.quantity = quantity;

				itemsToGive.put(itemId, itemForTrade);
			}

			targetPlayer.client.sendPacket(tradeAddRemovePacket);
			Centuria.logger.debug(MarkerManager.getMarker("TRADE"),
					"[TradeAddRemoveItem] [Add]  Server to client of player " + targetPlayer.account.getDisplayName()
							+ ": " + tradeAddRemovePacket.build());
		} else {
			var itemToReceive = itemsToReceive.get(itemId);

			if (itemToReceive != null) {
				itemToReceive.quantity += quantity;
			} else {
				ItemForTrade itemForTrade = new ItemForTrade();
				itemForTrade.item = item;
				itemForTrade.quantity = quantity;

				itemsToReceive.put(itemId, itemForTrade);
			}

			sourcePlayer.client.sendPacket(tradeAddRemovePacket);
			Centuria.logger.debug(MarkerManager.getMarker("TRADE"),
					"[TradeAddRemoveItem] [Add]  Server to client of player " + sourcePlayer.account.getDisplayName()
							+ ": " + tradeAddRemovePacket.build());
		}

	}

	/**
	 * Removes an item for trades.
	 * 
	 * @param player   The player who is removing the items.
	 * @param itemId   The player item id.
	 * @param quantity The quantity of the item to remove.
	 * @throws IOException
	 */
	public void removeItemFromTrade(Player player, String itemId, int quantity) throws IOException {
		// Check validity
		if (quantity < 0)
			return;

		// Check side and validity based on items to receive
		if (player.account.getAccountID().equals(sourcePlayer.account.getAccountID())
				&& !itemsToGive.containsKey(itemId))
			return;
		else if (player.account.getAccountID().equals(targetPlayer.account.getAccountID())
				&& !itemsToReceive.containsKey(itemId))
			return;

		// Verify quantity
		int currentQuant;
		if (player.account.getAccountID().equals(sourcePlayer.account.getAccountID()))
			currentQuant = itemsToGive.get(itemId).quantity;
		else
			currentQuant = itemsToReceive.get(itemId).quantity;

		// Reset to max if needed
		if (quantity > currentQuant)
			quantity = currentQuant;

		// Create packet
		TradeAddRemoveItemPacket tradeAddRemovePacket = new TradeAddRemoveItemPacket();
		tradeAddRemovePacket.isAdding = 0;
		tradeAddRemovePacket.success = true;
		tradeAddRemovePacket.inboundQuantity = quantity;
		tradeAddRemovePacket.quantity = quantity;
		tradeAddRemovePacket.userId = player.account.getAccountID();

		// Send to sender
		player.client.sendPacket(tradeAddRemovePacket);

		// Send to receiver
		if (player.account.getAccountID().equals(sourcePlayer.account.getAccountID())) {
			var item = itemsToGive.get(itemId);
			item.quantity -= quantity;
			if (item.quantity <= 0) {
				itemsToGive.remove(itemId);
			}
			tradeAddRemovePacket.updatedItem = item.item;
			targetPlayer.client.sendPacket(tradeAddRemovePacket);

			Centuria.logger.debug(MarkerManager.getMarker("TRADE"),
					"[TradeAddRemoveItem] [Remove] Server to client of player " + targetPlayer.account.getDisplayName()
							+ ": " + tradeAddRemovePacket.build());
		} else {
			var item = itemsToReceive.get(itemId);
			item.quantity -= quantity;
			if (item.quantity <= 0) {
				itemsToReceive.remove(itemId);
			}
			tradeAddRemovePacket.updatedItem = item.item;
			sourcePlayer.client.sendPacket(tradeAddRemovePacket);

			Centuria.logger.debug(MarkerManager.getMarker("TRADE"),
					"[TradeAddRemoveItem] [Remove] Server to client of player " + sourcePlayer.account.getDisplayName()
							+ ": " + tradeAddRemovePacket.build());
		}
	}

	/**
	 * Tells both players that a player has readied or vice versa during the trade.
	 * 
	 * @param playerReady The player who readied.
	 * @param isReady     If the player has readied, or vice versa.
	 * @throws IOException
	 */
	public void tradeReady(Player playerReady, int isReady) throws IOException {
		var tradeReadyPacket = new TradeReadyPacket();
		tradeReadyPacket.outbound_Success = true;
		tradeReadyPacket.outbound_UserId = playerReady.account.getAccountID();
		tradeReadyPacket.outbound_ReadyState = isReady;

		targetPlayer.client.sendPacket(tradeReadyPacket);
		sourcePlayer.client.sendPacket(tradeReadyPacket);

		Centuria.logger.debug(MarkerManager.getMarker("TRADE"), "[TradeReady] Server to client of player "
				+ targetPlayer.account.getDisplayName() + ": " + tradeReadyPacket.build());
		Centuria.logger.debug(MarkerManager.getMarker("TRADE"), "[TradeReady] Server to client of player "
				+ sourcePlayer.account.getDisplayName() + ": " + tradeReadyPacket.build());

	}

	/**
	 * Called when one player rejects the ready trade. Sends a message to both
	 * players to tell them to return to the normal trade screen.
	 * 
	 * @throws IOException
	 */
	public void tradeReadyReject() throws IOException {
		TradeReadyRejectPacket tradeReadyRejectPacket = new TradeReadyRejectPacket();

		targetPlayer.client.sendPacket(tradeReadyRejectPacket);
		sourcePlayer.client.sendPacket(tradeReadyRejectPacket);

		// Change the ready statuses back.
		this.readyStatusSource = false;
		this.readyStatusTarget = false;

		Centuria.logger.debug(MarkerManager.getMarker("TRADE"), "[TradeReadyReject] Server to client of player "
				+ targetPlayer.account.getDisplayName() + ": " + tradeReadyRejectPacket.build());
		Centuria.logger.debug(MarkerManager.getMarker("TRADE"), "[TradeReadyReject] Server to client of player "
				+ sourcePlayer.account.getDisplayName() + ": " + tradeReadyRejectPacket.build());
	}

	/**
	 * Called when one player accepts the trade.
	 * 
	 * @param player The player who accepted the trade.
	 * @throws IOException
	 */
	public void tradeReadyAccept(Player player) throws IOException {
		// Switch the accept value for the player who accepted..
		if (player.account.getAccountID().equals(sourcePlayer.account.getAccountID())) {
			this.readyStatusSource = true;
		} else {
			this.readyStatusTarget = true;
		}

		if (this.readyStatusTarget && this.readyStatusSource) {
			performTrade();
		} else {
			TradeReadyAcceptPacket tradeReadyAcceptPacket = new TradeReadyAcceptPacket();
			tradeReadyAcceptPacket.outbound_Success = true;

			if (player.account.getAccountID().equals(sourcePlayer.account.getAccountID())) {
				tradeReadyAcceptPacket.outbound_WaitingForOtherPlayer = true;
				sourcePlayer.client.sendPacket(tradeReadyAcceptPacket);
				Centuria.logger.debug(MarkerManager.getMarker("TRADE"), "[TradeReadyAccept] Server to client of player "
						+ targetPlayer.account.getDisplayName() + ": " + tradeReadyAcceptPacket.build());
			} else {
				tradeReadyAcceptPacket.outbound_WaitingForOtherPlayer = true;
				targetPlayer.client.sendPacket(tradeReadyAcceptPacket);
				Centuria.logger.debug(MarkerManager.getMarker("TRADE"), "[TradeReadyAccept] Server to client of player "
						+ sourcePlayer.account.getDisplayName() + ": " + tradeReadyAcceptPacket.build());
			}
		}
	}

	/**
	 * Performs the trade, giving items to both players and setting the trades they
	 * are engaged in to null.
	 */
	public void performTrade() {
		// Give and remove items..
		TradeReadyAcceptPacket tradeReadyAcceptPacket = new TradeReadyAcceptPacket();
		tradeReadyAcceptPacket.outbound_Success = true;
		tradeReadyAcceptPacket.outbound_WaitingForOtherPlayer = false;

		targetPlayer.client.sendPacket(tradeReadyAcceptPacket);
		sourcePlayer.client.sendPacket(tradeReadyAcceptPacket);
		for (var set : itemsToGive.entrySet()) {
			if (set.getValue().quantity > 1
					|| set.getValue().item.get("components").getAsJsonObject().has("Quantity")) {
				// items to give are source player..
				sourcePlayer.account.getSaveSpecificInventory().getItemAccessor(sourcePlayer)
						.remove(set.getValue().item.get("defId").getAsInt(), set.getValue().quantity);
				targetPlayer.account.getSaveSpecificInventory().getItemAccessor(targetPlayer)
						.add(set.getValue().item.get("defId").getAsInt(), set.getValue().quantity);
			} else {
				// lets support clothing/furniture items
				JsonObject itm = set.getValue().item;
				sourcePlayer.account.getSaveSpecificInventory().getItemAccessor(sourcePlayer).remove(itm);
				targetPlayer.account.getSaveSpecificInventory().getItemAccessor(targetPlayer).add(itm);
			}
		}

		for (var set : itemsToReceive.entrySet()) {
			if (set.getValue().quantity > 1
					|| set.getValue().item.get("components").getAsJsonObject().has("Quantity")) {
				// items to receive are target player..
				targetPlayer.account.getSaveSpecificInventory().getItemAccessor(targetPlayer)
						.remove(set.getValue().item.get("defId").getAsInt(), set.getValue().quantity);
				sourcePlayer.account.getSaveSpecificInventory().getItemAccessor(sourcePlayer)
						.add(set.getValue().item.get("defId").getAsInt(), set.getValue().quantity);
			} else {
				// lets support clothing/furniture items
				JsonObject itm = set.getValue().item;
				targetPlayer.account.getSaveSpecificInventory().getItemAccessor(targetPlayer).remove(itm);
				sourcePlayer.account.getSaveSpecificInventory().getItemAccessor(sourcePlayer).add(itm);
			}
		}
		targetPlayer.tradeEngagedIn = null;
		sourcePlayer.tradeEngagedIn = null;
	}
}
