package org.asf.centuria.accounts.impl;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.LevelInfo;

import com.google.gson.JsonObject;

public class InventoryBasedlevelInfo extends LevelInfo {

	private CenturiaAccount account;

	public InventoryBasedlevelInfo(CenturiaAccount account) {
		this.account = account;
	}

	@Override
	public boolean isLevelAvailable() {
		return true;
	}

	@Override
	public int getLevel() {
		// Retrieve level object
		if (!account.getPlayerInventory().containsItem("level")) {
			// Create object
		}
//		JsonObject level = account.getPlayerInventory().getItem("level").getAsJsonObject();
		return 0;
	}

	@Override
	public int getTotalXP() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getCurrentXP() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getLevelupXPCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void addXP(int xp) {
		// TODO Auto-generated method stub

	}

}
