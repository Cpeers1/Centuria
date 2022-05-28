package org.asf.emuferal.networking.chatserver.packets.chatcommands;

public abstract class AbstractChatCommand {

	/**
	 * Assigns the command ID
	 * 
	 * @return Command ID
	 */
	public abstract String id();

	/**
	 * Defines the permission node
	 * 
	 * @return Command permission node
	 */
	public abstract String permission();

}
