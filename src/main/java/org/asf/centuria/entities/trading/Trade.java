package org.asf.centuria.entities.trading;

import java.util.List;

import org.asf.centuria.entities.inventoryitems.InventoryItem;
import org.asf.centuria.entities.players.Player;

public class Trade {

	public Player targetUser;

	public Player initiatedByUser;

	public boolean isStarted;

	public List<InventoryItem> itemsToGive;

	public List<InventoryItem> itemsToReceive;

	public boolean readyStatusInitiater;

	public boolean readyStatusTarget;

	public boolean isConfirming;

	public Boolean isAcceptedByInitiater;

	public Boolean isAcceptedByTarget;

	public String chatConversationId;
	
	private Trade()
	{ }
	
	public static Trade StartNewTrade(Player initiatedByUser, Player targetUser)
	{
		//create a new trade
		Trade newTrade = new Trade();
		
		//set the trades engaged in for both players
		initiatedByUser.tradeEngagedIn = newTrade;
		targetUser.tradeEngagedIn = newTrade;
		
		// Set the trade properties
		newTrade.targetUser = targetUser;
		newTrade.initiatedByUser = initiatedByUser;
		newTrade.isStarted = false;
		newTrade.readyStatusInitiater = false;
		newTrade.readyStatusTarget = false;
		
		//
		
		
		return newTrade;
	}
}
