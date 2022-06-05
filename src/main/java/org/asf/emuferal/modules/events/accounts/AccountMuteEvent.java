package org.asf.emuferal.modules.events.accounts;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;

/**
 * 
 * Mute Event - called on account mute
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("accounts.mute")
public class AccountMuteEvent extends EventObject {

	private long unmuteTimestamp = -1;
	private EmuFeralAccount account;

	public AccountMuteEvent(EmuFeralAccount account, long unmuteTimestamp) {
		this.account = account;
		this.unmuteTimestamp = unmuteTimestamp;
	}

	@Override
	public String eventPath() {
		return "accounts.mute";
	}

	/**
	 * Retrieves the account that is being muted
	 * 
	 * @return EmuFeralAccount instance
	 */
	public EmuFeralAccount getAccount() {
		return account;
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
