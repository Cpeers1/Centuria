package org.asf.centuria.modules.events.accounts;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.modules.eventbus.EventObject;

/**
 * 
 * Deletion Event - called on account deletion
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class AccountDisplayNameChangedEvent extends EventObject {

	private CenturiaAccount account;
	private String oldDisplay;
	private String newDisplay;

	public AccountDisplayNameChangedEvent(CenturiaAccount account, String oldDisplay, String newDisplay) {
		this.account = account;
		this.oldDisplay = oldDisplay;
		this.newDisplay = newDisplay;
	}

	/**
	 * Retrieves the account that is being deleted
	 * 
	 * @return CenturiaAccount instance
	 */
	public CenturiaAccount getAccount() {
		return account;
	}

	/**
	 * Retrieves the old display name
	 * 
	 * @return Old display name
	 */
	public String getOldName() {
		return oldDisplay;
	}

	/**
	 * Retrieves the new display name
	 * 
	 * @return New display name
	 */
	public String getNewName() {
		return newDisplay;
	}

}
