package org.asf.centuria.modules.events.accounts;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.modules.eventbus.EventPath;

/**
 * 
 * Mute Event - called on account mute
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("accounts.mute")
public class AccountMuteEvent extends EventObject {

	private String reason;
	private String issuerID;
	private long unmuteTimestamp = -1;
	private CenturiaAccount account;

	public AccountMuteEvent(CenturiaAccount account, long unmuteTimestamp, String issuerID, String reason) {
		this.account = account;
		this.unmuteTimestamp = unmuteTimestamp;
		this.reason = reason;
		this.issuerID = issuerID;
	}

	@Override
	public String eventPath() {
		return "accounts.mute";
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
	 * Retrieves the account that is being muted
	 * 
	 * @return CenturiaAccount instance
	 */
	public CenturiaAccount getAccount() {
		return account;
	}

	/**
	 * Checks if the mute is indefinite
	 * 
	 * @return True if indefinite, false otherwise
	 */
	public boolean isPermanent() {
		return unmuteTimestamp == -1;
	}

	/**
	 * Retrieves the UNIX timestamp on which the account will be unmuted (UNIX
	 * timestamp in miliseconds)
	 * 
	 * @return Unmute timestamp in miliseconds
	 */
	public long getUnmuteTimestamp() {
		return unmuteTimestamp;
	}

}
