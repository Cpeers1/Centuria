package org.asf.centuria.enums.trading;

public enum TradeValidationType {

	Success(0), GenericError(-1), Timeout(-2), Rejected(-3), ItemNotAvailable(-4), UserNotAvaliable(-5), InventoryFull(-6), SenderCancelled(-7);

	public int value;

	TradeValidationType(int value) {
		this.value = value;
	}
	
}
