package org.asf.centuria.modules.events.accounts;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.modules.eventbus.EventPath;
import org.asf.centuria.networking.gameserver.GameServer;
import org.asf.centuria.networking.smartfox.SmartfoxClient;

import com.google.gson.JsonObject;

/**
 * 
 * Early Login Event - used to implement custom handshakes, called before
 * handling bans and other security checks.
 * 
 * @since Beta 1.5.3
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("accounts.prelogin")
public class AccountPreloginEvent extends EventObject {

	private GameServer server;
	private SmartfoxClient client;
	private CenturiaAccount account;
	private JsonObject params;
	private int status = 1;

	public AccountPreloginEvent(GameServer server, CenturiaAccount account, SmartfoxClient client, JsonObject params) {
		this.client = client;
		this.account = account;
		this.server = server;
		this.params = params;
	}

	@Override
	public String eventPath() {
		return "accounts.prelogin";
	}

	/**
	 * Retrieves the login response parameters, typically empty, used to add
	 * information to the login response message.
	 * 
	 * @return Login response parameters
	 */
	public JsonObject getLoginResponseParameters() {
		return params;
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
	 * @return CenturiaAccount instance
	 */
	public CenturiaAccount getAccount() {
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
