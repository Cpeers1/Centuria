package org.asf.emuferal.modules.events.accounts;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.players.Player;

/**
 * 
 * Login Success Event - called when a client successfully logs into the server
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("accounts.join")
public class LoginSuccessEvent extends EventObject {

	private Player player;
	private SmartfoxClient client;
	private EmuFeralAccount account;

	public LoginSuccessEvent(Player player, EmuFeralAccount account, SmartfoxClient client) {
		this.client = client;
		this.account = account;
		this.player = player;
	}

	@Override
	public String eventPath() {
		return "accounts.join";
	}

	/**
	 * Retrieves the player client
	 * 
	 * @return SmartfoxCient instance
	 */
	public SmartfoxClient getClient() {
		return client;
	}

	/**
	 * Retrieves the account that is being logged into
	 * 
	 * @return EmuFeralAccount instance
	 */
	public EmuFeralAccount getAccount() {
		return account;
	}

	/**
	 * Retrieves the player instance
	 * 
	 * @return Player instance
	 */
	public Player getPlayer() {
		return player;
	}

}
