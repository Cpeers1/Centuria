package org.asf.emuferal.enums.inventory;

public enum InspirationCombineStatus {
	Successful("success"),
	AlreadyOwned("already_owned"),
	InvalidCombo("invalid_combo");
	
	public String name;
	
	InspirationCombineStatus(String name)
	{
		this.name = name;
	}	
}