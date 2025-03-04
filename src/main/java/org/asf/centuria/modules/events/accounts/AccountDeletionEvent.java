package org.asf.centuria.modules.events.accounts;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.modules.eventbus.EventPath;

/**
 * 
 * Deletion Event - called on account deletion
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("accounts.delete")
public class AccountDeletionEvent extends EventObject {

	private CenturiaAccount account;

	public AccountDeletionEvent(CenturiaAccount account) {
		this.account = account;
	}

	@Override
	public String eventPath() {
		return "accounts.delete";
	}

	/**
	 * Retrieves the account that is being deleted
	 * 
	 * @return CenturiaAccount instance
	 */
	public CenturiaAccount getAccount() {
		return account;
	}

}
