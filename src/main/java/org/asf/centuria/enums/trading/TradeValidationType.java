package org.asf.centuria.enums.trading;

public enum TradeValidationType {

	Success(0), Generic_Error(-1), Timeout(-2), Rejected(-3), Item_Not_Avail(-4), User_Not_Avail(-5), Inventory_Full(-6), Sender_Cancelled(-7);

	public int value;

	TradeValidationType(int value) {
		this.value = value;
	}
	
}
