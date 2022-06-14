package org.asf.emuferal.modules.events.accounts;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;

/**
 * 
 * Ban Event - called on account ban
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("accounts.ban")
public class AccountBanEvent extends EventObject {

	private String reason;
	private String issuerID;
	private EmuFeralAccount account;
	private int days = -1;

	public AccountBanEvent(EmuFeralAccount account, int days, String issuerID, String reason) {
		this.account = account;
		this.days = days;
		this.reason = reason;
		this.issuerID = issuerID;
	}

	@Override
	public String eventPath() {
		return "accounts.ban";
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
	 * Retrieves the account that is being banned
	 * 
	 * @return EmuFeralAccount instance
	 */
	public EmuFeralAccount getAccount() {
		return account;
	}

	/**
	 * Checks if the ban is permanent
	 * 
	 * @return True if it is a permanent ban, false otherwise
	 */
	public boolean isPermanent() {
		return days == -1;
	}

	/**
	 * Retrieves the amount of days the account is banned for
	 * 
	 * @return Ban length in days or -1
	 */
	public int getDays() {
		return days;
	}
}
