package org.asf.emuferal.accounts.impl;

import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;

public class FileBasedAccountManager extends AccountManager {

	@Override
	public String authenticate(String username, char[] password) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String register(String username) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasPassword(String userID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean updatePassword(String userID, char[] password) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public EmuFeralAccount getAccount(String userID) {
		// TODO Auto-generated method stub
		return null;
	}

}
