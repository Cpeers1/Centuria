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

	public boolean readyStatusMe;

	public boolean readyStatusThem;

	public boolean isConfirming;

	public Boolean isAcceptedByMe;

	public Boolean isAcceptedByThem;

	public String chatConversationId;
}
