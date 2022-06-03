package org.asf.emuferal.modules.events.accounts;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;

/**
 * 
 * Pardon Event - called on account pardon
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("accounts.pardon")
public class AccountPardonEvent extends EventObject {

	private EmuFeralAccount account;

	public AccountPardonEvent(EmuFeralAccount account) {
		this.account = account;
	}

	@Override
	public String eventPath() {
		return "accounts.pardon";
	}

	/**
	 * Retrieves the account that is being banned
	 * 
	 * @return EmuFeralAccount instance
	 */
	public EmuFeralAccount getAccount() {
		return account;
	}

}
