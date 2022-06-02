package org.asf.emuferal.modules.events.accounts;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;

/**
 * 
 * Registration Event - called on account registration
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("accounts.register")
public class AccountRegistrationEvent extends EventObject {

	private EmuFeralAccount account;

	public AccountRegistrationEvent(EmuFeralAccount account) {
		this.account = account;
	}

	@Override
	public String eventPath() {
		return "accounts.register";
	}

	/**
	 * Retrieves the account being registered
	 * 
	 * @return EmuFeralAccount instance
	 */
	public EmuFeralAccount getAccount() {
		return account;
	}

}
