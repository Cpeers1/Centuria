package org.asf.centuria.modules.events.accounts;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.modules.eventbus.EventObject;

/**
 * 
 * Trade-ban Pardon Event - called when an account trade ban was removed
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class AccountTradePardonEvent extends EventObject {

	private String reason;
	private String issuerID;
	private CenturiaAccount account;

	public AccountTradePardonEvent(CenturiaAccount account, String issuerID, String reason) {
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

	/**
	 * Retrieves the account that is being pardoned
	 * 
	 * @return CenturiaAccount instance
	 */
	public CenturiaAccount getAccount() {
		return account;
	}

}
