package org.asf.centuria.modules.events.accounts;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.modules.eventbus.EventPath;

/**
 * 
 * Pardon Event - called on account pardon
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("accounts.pardon")
public class AccountPardonEvent extends EventObject {

	private String reason;
	private String issuerID;
	private CenturiaAccount account;

	public AccountPardonEvent(CenturiaAccount account, String issuerID, String reason) {
		this.account = account;
		this.reason = reason;
		this.issuerID = issuerID;
	}

	/**
	 * Retrieves the reason for the moderation action
	 *
	 * @return Action reason or null
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * Retrieves the action issuer
	 *
	 * @return Action issuer ID
	 */
	public String getIssuer() {
		return issuerID;
	}

	@Override
	public String eventPath() {
		return "accounts.pardon";
	}

	/**
	 * Retrieves the account that is being pardoned
	 * 
	 * @return CenturiaAccount instance
	 */
	public CenturiaAccount getAccount() {
		return account;
	}

}
