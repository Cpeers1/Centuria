package org.asf.emuferal.modules.events.accounts;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;

/**
 * 
 * Login Event - used to implement custom handshakes.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("accounts.login")
public class AccountLoginEvent extends EventObject {

	private GameServer server;
	private SmartfoxClient client;
	private EmuFeralAccount account;
	private int status = 1;

	public AccountLoginEvent(GameServer server, EmuFeralAccount account, SmartfoxClient client) {
		this.client = client;
		this.account = account;
		this.server = server;
	}

	@Override
	public String eventPath() {
		return "accounts.login";
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
	 * Retrieves the current login status code
	 * 
	 * @return Status code
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Assigns the login status code and marks the event as handled
	 * 
	 * @param status Login status code
	 */
	public void setStatus(int status) {
		setHandled();
		this.status = status;
	}

	/**
	 * Retrieves the game server
	 * 
	 * @return GameServer instance
	 */
	public GameServer getServer() {
		return server;
	}

}
