package org.asf.centuria.modules.events.accounts;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.modules.eventbus.EventPath;

/**
 * 
 * Kick Event - called on account kick
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("accounts.kick")
public class AccountKickEvent extends EventObject {

	private String reason;
	private String issuerID;
	private CenturiaAccount account;

	public AccountKickEvent(CenturiaAccount account, String issuerID, String reason) {
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
		return "accounts.kick";
	}

	/**
	 * Retrieves the account that is being kicked
	 * 
	 * @return CenturiaAccount instance
	 */
	public CenturiaAccount getAccount() {
		return account;
	}

}
