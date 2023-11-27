package org.asf.centuria.modules.events.servers;

import java.util.function.Consumer;

import org.asf.centuria.modules.eventbus.EventObject;
import org.asf.centuria.networking.chatserver.ChatServer;
import org.asf.centuria.networking.chatserver.networking.AbstractChatPacket;

/**
 * 
 * ChatServer Startup Event - used to handle startup of the chat server and
 * register packets via modules.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class ChatServerStartupEvent extends EventObject {

	private ChatServer server;
	private Consumer<AbstractChatPacket> registrationCommand;

	public ChatServerStartupEvent(ChatServer server, Consumer<AbstractChatPacket> registrationCommand) {
		this.server = server;
		this.registrationCommand = registrationCommand;
	}

	/**
	 * Retrieves the chat server
	 * 
	 * @return ChatServer instance
	 */
	public ChatServer getServer() {
		return server;
	}

	/**
	 * Registers packets on the chat server
	 * 
	 * @param packet Packet to register
	 */
	public void registerPacket(AbstractChatPacket packet) {
		registrationCommand.accept(packet);
	}

}
