package org.asf.emuferal.modules.events.servers;

import java.util.Map;
import java.util.function.Consumer;

import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;
import org.asf.emuferal.networking.chatserver.ChatServer;
import org.asf.emuferal.networking.chatserver.networking.AbstractChatPacket;

/**
 * 
 * ChatServer Startup Event - used to handle startup of the chat server and
 * register packets via modules.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("chatserver.startup")
public class ChatServerStartupEvent extends EventObject {

	private ChatServer server;
	private Consumer<AbstractChatPacket> registrationCommand;

	public ChatServerStartupEvent(ChatServer server, Consumer<AbstractChatPacket> registrationCommand) {
		this.server = server;
		this.registrationCommand = registrationCommand;
	}

	@Override
	public String eventPath() {
		return "chatserver.startup";
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

	@Override
	public Map<String, String> eventProperties() {
		return Map.of();
	}

}
