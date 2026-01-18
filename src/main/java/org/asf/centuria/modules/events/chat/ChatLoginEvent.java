package org.asf.centuria.modules.events.chat;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.ChatServer;

import com.google.gson.JsonObject;

/**
 * 
 * Chat Login Event - used to implement custom handshakes, called before
 * handling bans and other security checks.
 * 
 * @since Beta 1.5.3
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class ChatLoginEvent extends EventObject {

	private ChatServer server;
	private ChatClient client;
	private CenturiaAccount account;
	private JsonObject params;
	private boolean cancel;

	public ChatLoginEvent(ChatServer server, CenturiaAccount account, ChatClient client, JsonObject params) {
		this.client = client;
		this.account = account;
		this.server = server;
		this.params = params;
	}

	/**
	 * Cancels the event
	 */
	public void cancel() {
		cancel = true;
		setHandled();
	}

	/**
	 * Checks if the event was cancelled
	 * 
	 * @return True if cancelled, false otherwise
	 */
	public boolean isCancelled() {
		return cancel;
	}

	/**
	 * Retrieves the login request parameters. (strips token)
	 * 
	 * @return Login request parameters
	 */
	public JsonObject getLoginRequest() {
		return params;
	}

	/**
	 * Retrieves the chat client
	 * 
	 * @return ChatCient instance
	 */
	public ChatClient getClient() {
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
	 * Retrieves the chat server
	 * 
	 * @return ChatServer instance
	 */
	public ChatServer getServer() {
		return server;
	}

}
