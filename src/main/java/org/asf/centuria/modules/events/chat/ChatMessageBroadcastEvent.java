package org.asf.centuria.modules.events.chat;

import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.ChatServer;

/**
 * 
 * Chat Message Event - fired when a player sends a chat message, fired just
 * before it is passed on to other players, fired after chat filtering has been
 * performed.
 * 
 * @since Beta 1.5.3
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class ChatMessageBroadcastEvent extends EventObject {

	private ChatServer server;
	private ChatClient client;
	private CenturiaAccount account;
	private String message;
	private String conversation;
	private String conversationType;
	private boolean cancel;

	public ChatMessageBroadcastEvent(ChatServer server, CenturiaAccount account, ChatClient client, String message,
			String conversation, String conversationType) {
		this.client = client;
		this.account = account;
		this.server = server;
		this.message = message;
		this.conversation = conversation;
		this.conversationType = conversationType;
	}

	/**
	 * Cancels the event
	 */
	public void cancel() {
		cancel = true;
		setHandled();
	}

	/**
	 * Retrieves the conversation ID
	 * 
	 * @return Conversation room ID
	 */
	public String getConversationId() {
		return conversation;
	}

	/**
	 * Retrieves the conversation type
	 * 
	 * @return Conversation type string
	 */
	public String getConversationType() {
		return conversationType;
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
	 * Retrieves the chat message
	 * 
	 * @return Chat message string
	 */
	public String getMessage() {
		return message;
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
