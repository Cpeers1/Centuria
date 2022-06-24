package org.asf.emuferal.enums.inventory;

public enum InspirationCombineStatus {
	Successful(0),
	AlreadyOwned(1),
	InvalidCombo(2);
	
	public int value;
	
	InspirationCombineStatus(int value)
	{
		this.value = value;
	}	
}