package org.asf.emuferal.entities.systems.playervars;

import com.google.gson.JsonArray;

public class SetPlayerVarResult {
	public boolean success;
	public JsonArray changedVarInv;
	
	public SetPlayerVarResult(boolean success, JsonArray changedVarInv)
	{
		this.success = success;
		this.changedVarInv = changedVarInv;
	}
}
