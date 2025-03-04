package org.asf.centuria.modules.events.accounts;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.modules.eventbus.EventPath;

/**
 * 
 * Registration Event - called on account registration
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("accounts.register")
public class AccountRegistrationEvent extends EventObject {

	private CenturiaAccount account;

	public AccountRegistrationEvent(CenturiaAccount account) {
		this.account = account;
	}

	@Override
	public String eventPath() {
		return "accounts.register";
	}

	/**
	 * Retrieves the account being registered
	 * 
	 * @return CenturiaAccount instance
	 */
	public CenturiaAccount getAccount() {
		return account;
	}

}
