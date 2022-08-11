package org.asf.centuria.entities.uservars;

import org.asf.centuria.entities.inventoryitems.uservars.UserVarItem;

public class SetUserVarResult {
	public boolean success;
	public UserVarItem[] changedUserVars;
	
	public SetUserVarResult(boolean success, UserVarItem[] changedUserVars)
	{
		this.success = success;
		this.changedUserVars = changedUserVars;
	}
}
