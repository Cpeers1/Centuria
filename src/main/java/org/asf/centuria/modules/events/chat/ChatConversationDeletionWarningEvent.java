package org.asf.centuria.modules.events.chat;

import org.asf.centuria.modules.eventbus.EventObject;

/**
 * 
 * Chat Deletion Warning Event - fired when a chat is inactive for 30 days and as a result its slated for deletion.
 * 
 * @since Beta 1.8
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class ChatConversationDeletionWarningEvent extends EventObject {

	private String conversation;

	public ChatConversationDeletionWarningEvent(String conversation) {
		this.conversation = conversation;
	}

	/**
	 * Retrieves the conversation ID
	 * 
	 * @return Conversation room ID
	 */
	public String getConversationId() {
		return conversation;
	}

}
