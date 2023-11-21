package org.asf.centuria.networking.chatserver.networking;

import org.asf.centuria.networking.chatserver.ChatClient;
import org.asf.centuria.networking.chatserver.ChatServer;
import org.asf.centuria.networking.persistentservice.networking.AbstractPersistentServicePacket;

public abstract class AbstractChatPacket extends AbstractPersistentServicePacket<ChatClient, ChatServer> {

	/**
	 * Creates a new packet instance
	 * 
	 * @return AbstractChatPacket instance
	 */
	public abstract AbstractChatPacket instantiate();

	/**
	 * Handles the packet
	 */
	public abstract boolean handle(ChatClient client);

}
