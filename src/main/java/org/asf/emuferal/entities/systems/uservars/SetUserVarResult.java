package org.asf.emuferal.entities.systems.uservars;

import org.asf.emuferal.entities.inventory.uservars.UserVarItem;

public class SetUserVarResult {
	public boolean success;
	public UserVarItem[] changedUserVars;
	
	public SetUserVarResult(boolean success, UserVarItem[] changedUserVars)
	{
		this.success = success;
		this.changedUserVars = changedUserVars;
	}
}
