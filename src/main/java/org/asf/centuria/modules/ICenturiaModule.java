package org.asf.centuria.modules;

import java.net.ServerSocket;

import org.asf.centuria.modules.eventbus.IEventReceiver;
import org.asf.centuria.networking.chatserver.ChatServer;
import org.asf.centuria.networking.gameserver.GameServer;

public interface ICenturiaModule extends IEventReceiver {

	/**
	 * Defines the module ID
	 */
	public String id();

	/**
	 * Defines the module version
	 */
	public String version();

	/**
	 * Main initialization method
	 */
	public void init();

	/**
	 * Early loading method
	 */
	public default void preInit() {
	}

	/**
	 * Post-loading method
	 */
	public default void postInit() {
	}

	/**
	 * Called for modules to allow them to replace the game server implementation or
	 * to extend it
	 * 
	 * @param socket Server socket
	 * @return GameServer instance or null
	 */
	public default GameServer replaceGameServer(ServerSocket socket) {
		return null;
	}

	/**
	 * Called for modules to allow them to replace the chat server implementation or
	 * to extend it
	 * 
	 * @param socket Server socket
	 * @return ChatServer instance or null
	 */
	public default ChatServer replaceChatServer(ServerSocket socket) {
		return null;
	}

}
