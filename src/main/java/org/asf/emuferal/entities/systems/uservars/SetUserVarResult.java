package org.asf.emuferal.entities.systems.uservars;

import org.asf.emuferal.entities.inventory.components.uservars.UserVarComponent;

import com.google.gson.JsonArray;

public class SetUserVarResult {
	public boolean success;
	public UserVarComponent[] changedUserVars;
	
	public SetUserVarResult(boolean success, UserVarComponent[] changedUserVars)
	{
		this.success = success;
		this.changedUserVars = changedUserVars;
	}
}
