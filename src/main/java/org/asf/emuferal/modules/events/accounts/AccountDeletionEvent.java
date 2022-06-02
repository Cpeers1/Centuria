package org.asf.emuferal.modules.events.accounts;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;

/**
 * 
 * Deletion Event - called on account deletion
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("accounts.delete")
public class AccountDeletionEvent extends EventObject {

	private EmuFeralAccount account;

	public AccountDeletionEvent(EmuFeralAccount account) {
		this.account = account;
	}

	@Override
	public String eventPath() {
		return "accounts.delete";
	}

	/**
	 * Retrieves the account that is being deleted
	 * 
	 * @return EmuFeralAccount instance
	 */
	public EmuFeralAccount getAccount() {
		return account;
	}

}
