package org.asf.centuria.accounts.impl;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.PlayerInventory;
import org.asf.centuria.accounts.SaveMode;
import org.asf.centuria.accounts.SaveSettings;

import com.google.gson.JsonElement;

public class SelectiveInventory extends PlayerInventory {

	private CenturiaAccount account;

	public SelectiveInventory(CenturiaAccount account) {
		this.account = account;
	}

	@Override
	public JsonElement getItem(String itemID) {
		if (account.getSaveMode() == SaveMode.SINGLE || account.getSaveSharedInventory().containsItem(itemID))
			return account.getSaveSharedInventory().getItem(itemID);
		return account.getSaveSpecificInventory().getItem(itemID);
	}

	@Override
	public void setItem(String itemID, JsonElement itemData) {
		if (account.getSaveMode() == SaveMode.SINGLE || account.getSaveSharedInventory().containsItem(itemID))
			account.getSaveSharedInventory().setItem(itemID, itemData);
		else
			account.getSaveSpecificInventory().setItem(itemID, itemData);
	}

	@Override
	public void deleteItem(String itemID) {
		if (account.getSaveMode() == SaveMode.SINGLE || account.getSaveSharedInventory().containsItem(itemID))
			account.getSaveSharedInventory().deleteItem(itemID);
		else
			account.getSaveSpecificInventory().deleteItem(itemID);
	}

	@Override
	public boolean containsItem(String itemID) {
		if (account.getSaveMode() == SaveMode.SINGLE)
			return account.getSaveSharedInventory().containsItem(itemID);
		if (account.getSaveSharedInventory().containsItem(itemID))
			return true;
		return account.getSaveSpecificInventory().containsItem(itemID);
	}

	@Override
	public SaveSettings getSaveSettings() {
		if (account.getSaveSpecificInventory().containsItem("savesettings"))
			return account.getSaveSpecificInventory().getSaveSettings();
		return account.getSaveSharedInventory().getSaveSettings();
	}

	@Override
	public void writeSaveSettings() {
		account.getSaveSpecificInventory().writeSaveSettings();
	}

}
