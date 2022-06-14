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
@EventPath("accounts.kick")
public class AccountKickEvent extends EventObject {

	private String reason;
	private String issuerID;
	private EmuFeralAccount account;

	public AccountKickEvent(EmuFeralAccount account, String issuerID, String reason) {
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
	 * @return EmuFeralAccount instance
	 */
	public EmuFeralAccount getAccount() {
		return account;
	}

}
