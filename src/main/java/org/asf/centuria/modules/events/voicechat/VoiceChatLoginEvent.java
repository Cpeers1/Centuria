package org.asf.centuria.modules.events.voicechat;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.modules.eventbus.EventPath;
import org.asf.centuria.networking.voicechatserver.VoiceChatClient;
import org.asf.centuria.networking.voicechatserver.VoiceChatServer;

import com.google.gson.JsonObject;

/**
 * 
 * Voice Chat Login Event - used to implement custom handshakes, called before
 * handling bans and other security checks.
 * 
 * @since Beta 1.7.2
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("voicechat.login")
public class VoiceChatLoginEvent extends EventObject {

	private VoiceChatServer server;
	private VoiceChatClient client;
	private CenturiaAccount account;
	private JsonObject params;
	private boolean cancel;

	public VoiceChatLoginEvent(VoiceChatServer server, CenturiaAccount account, VoiceChatClient client,
			JsonObject params) {
		this.client = client;
		this.account = account;
		this.server = server;
		this.params = params;
	}

	@Override
	public String eventPath() {
		return "voicechat.login";
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
	 * Retrieves the voice chat client
	 * 
	 * @return VoiceChatCient instance
	 */
	public VoiceChatClient getClient() {
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
	 * Retrieves the voice chat server
	 * 
	 * @return VoiceChatServer instance
	 */
	public VoiceChatServer getServer() {
		return server;
	}

}
